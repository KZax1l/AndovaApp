package com.andova.app.music.player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;

import com.andova.app.music.model.MusicPlaybackTrack;
import com.andova.app.util.MusicUtil;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.andova.app.AndovaApplication.LOGGER;
import static com.andova.app.music.MusicActivity.EXTRA_INTENT_MUSIC_COVER_URI;
import static com.andova.app.music.MusicActivity.RECEIVER_ACTION_UPDATE_MUSIC_COVER;
import static com.andova.app.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_FADE_DOWN;
import static com.andova.app.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_FADE_UP;

/**
 * Created by Administrator on 2018-02-27.
 *
 * @author kzaxil
 * @since 1.0.0
 */
class MusicPlayer {
    private static final int IDLE_DELAY = 5 * 60 * 1000;
    private ArrayList<MusicPlaybackTrack> mPlaylist = new ArrayList<>(100);

    private int mPlayPos = -1;
    private int mNextPlayPos = -1;
    private String mFileToPlay;
    private long mLastPlayedTime;
    private boolean mIsSupposedToBePlaying = false;

    private MusicService mService;
    private MediaTracker mMediaTracker;
    private MediaDataSource mDataSource;

    MusicPlayer(MusicService service, MediaTracker mediaTracker, MediaDataSource dataSource) {
        mService = service;
        mDataSource = dataSource;
        mMediaTracker = mediaTracker;
    }

    /**
     * 设置播放列表
     *
     * @param position -1标志随机播放
     */
    void open(Service context, final long[] list, final int position, long sourceId) {
        synchronized (this) {
            final int length = list.length;
            boolean isNewList = true;
            if (mPlaylist.size() == length) {
                isNewList = false;
                for (int i = 0; i < length; i++) {
                    if (list[i] == mPlaylist.get(i).mId) continue;
                    isNewList = true;
                    break;
                }
            }
            if (isNewList) {
                addToPlayList(list, -1, sourceId); //添加到播放列表,并清空原播放列表
            }
            if (position >= 0) mPlayPos = position;
            openCurrentAndNext(context);
        }
    }

    /**
     * 停止播放
     *
     * @param goToIdle 是否准备关闭Service
     */
    private void stop(Service service, final boolean goToIdle) {
        LOGGER.info("Stopping playback, goToIdle = " + goToIdle);
        if (mMediaTracker.isInitialized()) mMediaTracker.stop();
        mFileToPlay = null;
        closeCursor();
        if (goToIdle) {
            setIsSupposedToBePlaying(false, false);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                service.stopForeground(false);
            else service.stopForeground(true);
        }
    }

    /**
     * 播放歌曲
     *
     * @param createNewNextTrack 设置下个曲目时是否重新产生序号
     */
    private boolean play(Context context, AudioManager audioManager, AudioManager.OnAudioFocusChangeListener listener, MusicPlayerHandler handler, boolean createNewNextTrack) {
        int status = audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        LOGGER.info("Starting playback: audio focus request status = " + status);

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false;

        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);

        if (createNewNextTrack) {
            setNextTrack(); //重新产生mNextPlayPos
        } else {
            setNextTrack(mNextPlayPos);//不重新产生mNextPlayPos
        }

        if (mMediaTracker.isInitialized()) {
            mMediaTracker.start();
            handler.removeMessages(MEDIA_PLAYER_CODE_FADE_DOWN);
            handler.sendEmptyMessage(MEDIA_PLAYER_CODE_FADE_UP); // 组件调到正常音量

            setIsSupposedToBePlaying(true, true);
            return true;
        }
        return false;
    }

    void stop(Service service) {
        stop(service, true);
    }

    boolean play(Context context, AudioManager audioManager, AudioManager.OnAudioFocusChangeListener listener, MusicPlayerHandler handler) {
        return play(context, audioManager, listener, handler, true);
    }

    /**
     * 暂停播放
     */
    boolean pause(final Context context, MusicPlayerHandler handler) {
        LOGGER.info("Pausing playback");
        synchronized (this) {
            handler.removeMessages(MEDIA_PLAYER_CODE_FADE_UP);
            handler.sendEmptyMessage(MEDIA_PLAYER_CODE_FADE_DOWN);
            if (!mIsSupposedToBePlaying) return false;
            setIsSupposedToBePlaying(false, true);
            TimerTask task = new TimerTask() {
                public void run() {
                    final Intent intent = new Intent(
                            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                    intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
                    context.sendBroadcast(intent); //由系统接收,通知系统audio_session将关闭,不再使用音效

                    mMediaTracker.pause();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 200);
            return true;
        }
    }

    /**
     * 切换到前一首歌曲
     */
    void previous(Service service, AudioManager audioManager, AudioManager.OnAudioFocusChangeListener listener, MusicPlayerHandler handler) {
        synchronized (this) {
            int pos = getPreviousPlayPosition();
            if (pos < 0) return;
            mNextPlayPos = mPlayPos;
            mPlayPos = pos;
            stop(service, false);
            openCurrent(service);
            play(service, audioManager, listener, handler, false); //不产生新的下首序号
        }
    }

    /**
     * 播放下一首
     *
     * @param force 控制播放列表播放完时是否重新开始
     */
    void next(Service service, AudioManager audioManager, AudioManager.OnAudioFocusChangeListener listener,
              MusicPlayerHandler handler, final boolean force) {
        LOGGER.info("Going to next track");
        synchronized (this) {
            if (mPlaylist.size() <= 0) {
                LOGGER.info("No play queue");
                return;
            }
            int pos;
            if (force) {
                pos = getNextPosition(true);
            } else {
                pos = mNextPlayPos;
                if (pos < 0) pos = getNextPosition(false);
            }

            if (pos < 0) { //无法选取下一首歌
                setIsSupposedToBePlaying(false, true);
                return;
            }

            setAndRecordPlayPos(pos); //设置当前播放曲目的ID
            openCurrentAndNext(service);
            play(service, audioManager, listener, handler, true); //不重新产生mNextPlayPos
        }
    }

    /**
     * 给mPlayPos设置播放的曲目ID
     */
    private void setAndRecordPlayPos(int nextPos) {
        synchronized (this) {
            mPlayPos = nextPos;
        }
    }

    /**
     * 获取播放列表中的前一首歌曲
     */
    private int getPreviousPlayPosition() {
        synchronized (this) {
            return mPlayPos > 0 ? mPlayPos - 1 : mPlaylist.size() > 0 ? mPlaylist.size() - 1 : 0;
        }
    }

    boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /**
     * 是否正在播放，或距离上次播放不超过5分钟
     */
    boolean recentlyPlayed() {
        return isPlaying() || System.currentTimeMillis() - mLastPlayedTime < IDLE_DELAY;
    }

    private void setIsSupposedToBePlaying(boolean value, boolean notify) {
        if (mIsSupposedToBePlaying == value) return;
        mIsSupposedToBePlaying = value;
        if (!mIsSupposedToBePlaying) mLastPlayedTime = System.currentTimeMillis();
    }

    /**
     * 在指定位置插入播放列表
     *
     * @param position -1 清空原播放列表
     */
    private void addToPlayList(final long[] list, int position, long sourceId) {
        final int addLength = list.length;
        if (position < 0) {
            mPlaylist.clear();
            position = 0;
        }

        mPlaylist.ensureCapacity(mPlaylist.size() + addLength);
        if (position > mPlaylist.size()) {
            position = mPlaylist.size();
        }

        final ArrayList<MusicPlaybackTrack> arrayList = new ArrayList<>(addLength);
        for (int i = 0; i < list.length; i++) {
            arrayList.add(new MusicPlaybackTrack(list[i], sourceId, i));
        }

        mPlaylist.addAll(position, arrayList);
    }

    private void openCurrent(Service service) {
        openCurrentAndMaybeNext(service, false);
    }

    private void openCurrentAndNext(Service service) {
        openCurrentAndMaybeNext(service, true);
    }

    /**
     * 准备当前或者下一首能够播放的曲目(设置{@link MediaPlayer})
     *
     * @param openNext 是否给player提前设置下一首
     */
    private void openCurrentAndMaybeNext(Service service, final boolean openNext) {
        synchronized (this) {
            if (mPlaylist.size() == 0) return;
            stop(service, false);

            mDataSource.updateCursor(service, mPlaylist.get(mPlayPos).mId);
            while (true) {
                if (mDataSource.getAudioCursor() != null
                        && openFile(service, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/"
                        + mDataSource.getAudioCursor().getLong(0))) { //如果成功读取当前曲目的信息,结束循环 设置player和fileToPlay
                    break;
                }
            }
            if (openNext) { //提前设置好下一首曲目和预加载
                setNextTrack();
            }
        }
    }

    /**
     * 根据path通过多种方式来获取歌曲的信息,初始化player
     */
    private boolean openFile(Context context, final String path) {
        LOGGER.info("path = " + path);
        synchronized (this) {
            if (path == null) return false;

            if (mDataSource.getAudioCursor() == null) {
                Uri uri = Uri.parse(path);
                boolean shouldAddToPlaylist = true;
                long id = -1;
                try {
                    id = Long.valueOf(uri.getLastPathSegment());
                } catch (NumberFormatException ex) {
                    // Ignore
                }

                if (id != -1 && path.startsWith(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                    mDataSource.updateCursor(context, uri);
                } else if (id != -1 && path.startsWith(
                        MediaStore.Files.getContentUri("external").toString())) {
                    mDataSource.updateCursor(context, id);
                } else if (path.startsWith("content://downloads/")) {
                } else {
                    String where = MediaStore.Audio.Media.DATA + "=?";
                    String[] selectionArgs = new String[]{path};
                    mDataSource.updateCursor(context, where, selectionArgs);
                }
                try {
                    if (mDataSource.getAudioCursor() != null && shouldAddToPlaylist) {
                        mPlaylist.clear();
                        mPlaylist.add(new MusicPlaybackTrack(
                                mDataSource.getAudioCursor().getLong(0), -1, -1));
                        mPlayPos = 0;
                    }
                } catch (final UnsupportedOperationException ex) {
                    // Ignore
                }
            }

            mFileToPlay = path;
            mMediaTracker.setDataSource(mFileToPlay);
            if (mMediaTracker.isInitialized()) {
                return true;
            }
            return false;
        }
    }

    void setVolume(final float vol) {
        mMediaTracker.setVolume(vol);
    }

    void closeCursor() {
        mDataSource.closeCursor();
    }

    /**
     * @param force 控制播放列表播放完时是否重新开始
     * @return -1表示无法确定下一曲目位置
     */
    private int getNextPosition(final boolean force) {
        return mPlayPos >= mPlaylist.size() - 1 ? 0 : mPlayPos + 1;
    }

    private void setNextTrack() {
        setNextTrack(getNextPosition(false));
    }

    /**
     * 设置下首播放曲目的位置,并设置{@link MediaPlayer}下次播放的DataSource
     */
    private void setNextTrack(int position) {
        mNextPlayPos = position;
        LOGGER.info("Next play position = " + mNextPlayPos);
        if (mNextPlayPos >= 0 && mPlaylist != null && mNextPlayPos < mPlaylist.size()) {
            final long id = mPlaylist.get(mNextPlayPos).mId;
            mMediaTracker.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
            Intent intent = new Intent(RECEIVER_ACTION_UPDATE_MUSIC_COVER);
            intent.putExtra(EXTRA_INTENT_MUSIC_COVER_URI, MusicUtil.getAlbumArtUri(mDataSource.getAlbumId()).toString());
            LocalBroadcastManager.getInstance(mService).sendBroadcast(intent);
        } else {
            mMediaTracker.setNextDataSource(null);
        }
    }

    /**
     * 当前曲目播完，准备播放下一首
     */
    void goToNext() {
        LOGGER.info("Go to next music");
        setAndRecordPlayPos(mNextPlayPos);
        setNextTrack();
        mDataSource.updateCursor(mService, mPlaylist.get(mPlayPos).mId);
        mService.updateNotification();
    }
}

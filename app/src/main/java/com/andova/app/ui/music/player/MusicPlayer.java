package com.andova.app.ui.music.player;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.andova.app.ui.music.model.MusicPlaybackTrack;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.andova.app.ui.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_FADE_DOWN;
import static com.andova.app.ui.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_FADE_UP;

/**
 * Created by Administrator on 2018-02-27.
 *
 * @author kzaxil
 * @since 1.0.0
 */
class MusicPlayer {
    private static final String TAG = MusicPlayer.class.getSimpleName();
    private static final int IDLE_DELAY = 5 * 60 * 1000;
    private ArrayList<MusicPlaybackTrack> mPlaylist = new ArrayList<>(100);

    private int mPlayPos = -1;
    private String mFileToPlay;
    private long mLastPlayedTime;
    private boolean mIsSupposedToBePlaying = false;

    private MediaTracker mMediaTracker;
    private MediaDataSource mDataSource;

    MusicPlayer(MediaTracker mediaTracker, MediaDataSource dataSource, MusicPlayerHandler handler) {
        mMediaTracker = mediaTracker;
        mDataSource = dataSource;
    }

    /**
     * 设置播放列表
     *
     * @param position -1标志随机播放
     */
    void open(Context context, final long[] list, final int position, long sourceId) {
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
    private void stop(final boolean goToIdle) {
        Log.d(TAG, "Stopping playback, goToIdle = " + goToIdle);
        if (mMediaTracker.isInitialized()) mMediaTracker.stop();
        mFileToPlay = null;
    }

    /**
     * 播放歌曲
     *
     * @param createNewNextTrack 设置下个曲目时是否重新产生序号
     */
    private boolean play(Context context, AudioManager audioManager, AudioManager.OnAudioFocusChangeListener listener, MusicPlayerHandler handler, boolean createNewNextTrack) {
        int status = audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        Log.d(TAG, "Starting playback: audio focus request status = " + status);

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false;

        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);

        if (mMediaTracker.isInitialized()) {
            mMediaTracker.start();
            handler.removeMessages(MEDIA_PLAYER_CODE_FADE_DOWN);
            handler.sendEmptyMessage(MEDIA_PLAYER_CODE_FADE_UP); // 组件调到正常音量

            setIsSupposedToBePlaying(true, true);
            return true;
        }
        return false;
    }

    void stop() {
        stop(true);
    }

    boolean play(Context context, AudioManager audioManager, AudioManager.OnAudioFocusChangeListener listener, MusicPlayerHandler handler) {
        return play(context, audioManager, listener, handler, true);
    }

    /**
     * 暂停播放
     */
    boolean pause(final Context context, MusicPlayerHandler handler) {
        Log.d(TAG, "Pausing playback");
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

        final ArrayList<MusicPlaybackTrack> arrayList = new ArrayList<MusicPlaybackTrack>(addLength);
        for (int i = 0; i < list.length; i++) {
            arrayList.add(new MusicPlaybackTrack(list[i], sourceId, i));
        }

        mPlaylist.addAll(position, arrayList);
    }

    private void openCurrentAndNext(Context context) {
        openCurrentAndMaybeNext(context, true);
    }

    /**
     * 准备当前或者下一首能够播放的曲目(设置{@link MediaPlayer})
     *
     * @param openNext 是否给player提前设置下一首
     */
    private void openCurrentAndMaybeNext(Context context, final boolean openNext) {
        synchronized (this) {
            mDataSource.closeCursor();

            if (mPlaylist.size() == 0) {
                return;
            }
            stop(false);

            mDataSource.updateCursor(context, mPlaylist.get(mPlayPos).mId);
            while (true) {
                if (mDataSource.getAudioCursor() != null
                        && openFile(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/"
                        + mDataSource.getAudioCursor().getLong(0))) { //如果成功读取当前曲目的信息,结束循环 设置player和fileToPlay
                    break;
                }
            }
        }
    }

    /**
     * 根据path通过多种方式来获取歌曲的信息,初始化player
     */
    private boolean openFile(Context context, final String path) {
        Log.d(TAG, "openFile: path = " + path);
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
}

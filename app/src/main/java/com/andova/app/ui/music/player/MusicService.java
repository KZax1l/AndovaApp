package com.andova.app.ui.music.player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;

import com.andova.app.ui.music.IMusicServiceAPI;
import com.andova.app.ui.music.model.MusicPlaybackTrack;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2018-02-24.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MusicService extends Service {
    private static final String TAG = MusicService.class.getSimpleName();

    private static final String[] MUSIC_PROJECTION = new String[]{ //歌曲信息
            "audio._id AS _id", MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };
    private static final String[] ALBUM_PROJECTION = new String[]{ //专辑信息
            MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR
    };

    /**
     * {@link MediaPlayer}播放完毕且暂时没有下一首
     */
    private static final int MEDIA_PLAYER_ACTION_ENDED = 1;
    /**
     * {@link MediaPlayer}播放完毕切换到下一首
     */
    private static final int MEDIA_PLAYER_ACTION_WENT_TO_NEXT = 2;
    /**
     * 释放{@link android.os.PowerManager.WakeLock}
     */
    private static final int MEDIA_PLAYER_ACTION_RELEASE_WAKELOCK = 3;
    /**
     * {@link MediaPlayer}播放出错
     */
    private static final int MEDIA_PLAYER_ACTION_SERVER_DIED = 4;
    /**
     * 音频焦点变化
     */
    private static final int MEDIA_PLAYER_ACTION_FOCUS_CHANGED = 5;
    /**
     * 降低音量
     */
    private static final int MEDIA_PLAYER_ACTION_FADE_DOWN = 6;
    /**
     * 增加音量
     */
    private static final int MEDIA_PLAYER_ACTION_FADE_UP = 7;

    private int mPlayPos = -1;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mIsSupposedToBePlaying = false;
    private ArrayList<MusicPlaybackTrack> mPlaylist = new ArrayList<>(100);

    private final IBinder mBinder = new ServiceStub(this);

    private String mFileToPlay;

    private Cursor mCursor;
    private Cursor mAlbumCursor;
    private MultiPlayer mPlayer;
    private AudioManager mAudioManager;
    private HandlerThread mHandlerThread;
    private MusicPlayerHandler mPlayerHandler;

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() { //监听,转发给mPlayerHandler处理

        @Override
        public void onAudioFocusChange(final int focusChange) {
            mPlayerHandler.obtainMessage(MEDIA_PLAYER_ACTION_FOCUS_CHANGED, focusChange, 0).sendToTarget();
        }
    };

    @Override
    public IBinder onBind(final Intent intent) {
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        Log.d(TAG, "Service unbound");
        mServiceInUse = false;

        if (mIsSupposedToBePlaying) return true;
        stopSelf(mServiceStartId);
        return true;
    }

    @Override
    public void onRebind(final Intent intent) {
        mServiceInUse = true;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating service");
        super.onCreate();

        mHandlerThread = new HandlerThread("MusicPlayerHandler", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mPlayerHandler = new MusicPlayerHandler(this, mHandlerThread.getLooper()); // 处理播放状态相关

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mPlayer = new MultiPlayer(this); // 当Player接收到播放状态相关的回调时,发送信息给Handler处理
        mPlayer.setHandler(mPlayerHandler);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service");
        super.onDestroy();

        mPlayerHandler.removeCallbacksAndMessages(null);

        mPlayer.release();
        mPlayer = null;

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.d(TAG, "Got new intent " + intent + ", startId = " + startId);
        mServiceStartId = startId;

        return START_NOT_STICKY;
    }

    /**
     * 设置播放列表
     *
     * @param position -1标志随机播放
     */
    public void open(final long[] list, final int position, long sourceId) {
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
            openCurrentAndNext();
        }
    }

    /**
     * 停止播放
     *
     * @param goToIdle 是否准备关闭Service
     */
    private void stop(final boolean goToIdle) {
        Log.d(TAG, "Stopping playback, goToIdle = " + goToIdle);
        if (mPlayer.isInitialized()) mPlayer.stop();
        mFileToPlay = null;
    }

    /**
     * 播放歌曲
     *
     * @param createNewNextTrack 设置下个曲目时是否重新产生序号
     */
    public void play(boolean createNewNextTrack) {
        int status = mAudioManager.requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        Log.d(TAG, "Starting playback: audio focus request status = " + status);

        if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;

        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(intent);

        if (mPlayer.isInitialized()) {
            mPlayer.start();
            mPlayerHandler.removeMessages(MEDIA_PLAYER_ACTION_FADE_DOWN);
            mPlayerHandler.sendEmptyMessage(MEDIA_PLAYER_ACTION_FADE_UP); // 组件调到正常音量

            setIsSupposedToBePlaying(true, true);
        }
    }

    public void stop() {
        stop(true);
    }

    public void play() {
        play(true);
    }

    /**
     * 暂停播放
     */
    public void pause() {
        Log.d(TAG, "Pausing playback");
        synchronized (this) {
            mPlayerHandler.removeMessages(MEDIA_PLAYER_ACTION_FADE_UP);
            mPlayerHandler.sendEmptyMessage(MEDIA_PLAYER_ACTION_FADE_DOWN);
            if (!mIsSupposedToBePlaying) return;
            setIsSupposedToBePlaying(false, true);
            TimerTask task = new TimerTask() {
                public void run() {
                    final Intent intent = new Intent(
                            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                    intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                    sendBroadcast(intent); //由系统接收,通知系统audio_session将关闭,不再使用音效

                    mPlayer.pause();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 200);
        }
    }

    private void setIsSupposedToBePlaying(boolean value, boolean notify) {
        if (mIsSupposedToBePlaying == value) return;
        mIsSupposedToBePlaying = value;
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

    private void updateCursor(final long trackId) {
        updateCursor("_id=" + trackId, null);
    }

    /**
     * 更新播放曲目和专辑的相关信息
     */
    private void updateCursor(final String selection, final String[] selectionArgs) {
        synchronized (this) {
            closeCursor(); //关闭mCursor和mAlbumCursor
            mCursor = openCursorAndGoToFirst(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MUSIC_PROJECTION, selection, selectionArgs);
        }
        updateAlbumCursor();
    }

    private void updateCursor(final Uri uri) {
        synchronized (this) {
            closeCursor();
            mCursor = openCursorAndGoToFirst(uri, MUSIC_PROJECTION, null, null);
        }
        updateAlbumCursor();
    }

    private void updateAlbumCursor() {
        long albumId = getAlbumId();
        if (albumId >= 0) {
            mAlbumCursor = openCursorAndGoToFirst(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    ALBUM_PROJECTION, "_id=" + albumId, null);
        } else {
            mAlbumCursor = null;
        }
    }

    private Cursor openCursorAndGoToFirst(Uri uri, String[] projection,
                                          String selection, String[] selectionArgs) {
        Cursor c = getContentResolver().query(uri, projection,
                selection, selectionArgs, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    private synchronized void closeCursor() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        if (mAlbumCursor != null) {
            mAlbumCursor.close();
            mAlbumCursor = null;
        }
    }

    private void openCurrentAndNext() {
        openCurrentAndMaybeNext(true);
    }

    /**
     * 准备当前或者下一首能够播放的曲目(设置{@link MediaPlayer})
     *
     * @param openNext 是否给player提前设置下一首
     */
    private void openCurrentAndMaybeNext(final boolean openNext) {
        synchronized (this) {
            closeCursor();

            if (mPlaylist.size() == 0) {
                return;
            }
            stop(false);

            updateCursor(mPlaylist.get(mPlayPos).mId);
            while (true) {
                if (mCursor != null
                        && openFile(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/"
                        + mCursor.getLong(0))) { //如果成功读取当前曲目的信息,结束循环 设置player和fileToPlay
                    break;
                }
            }
        }
    }

    /**
     * 根据path通过多种方式来获取歌曲的信息,初始化player
     */
    public boolean openFile(final String path) {
        Log.d(TAG, "openFile: path = " + path);
        synchronized (this) {
            if (path == null) return false;

            if (mCursor == null) {
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
                    updateCursor(uri);
                } else if (id != -1 && path.startsWith(
                        MediaStore.Files.getContentUri("external").toString())) {
                    updateCursor(id);
                } else if (path.startsWith("content://downloads/")) {
                } else {
                    String where = MediaStore.Audio.Media.DATA + "=?";
                    String[] selectionArgs = new String[]{path};
                    updateCursor(where, selectionArgs);
                }
                try {
                    if (mCursor != null && shouldAddToPlaylist) {
                        mPlaylist.clear();
                        mPlaylist.add(new MusicPlaybackTrack(
                                mCursor.getLong(0), -1, -1));
                        mPlayPos = 0;
                    }
                } catch (final UnsupportedOperationException ex) {
                    // Ignore
                }
            }

            mFileToPlay = path;
            mPlayer.setDataSource(mFileToPlay);
            if (mPlayer.isInitialized()) {
                return true;
            }
            return false;
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            if (mCursor == null) return -1;
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
        }
    }

    /**
     * 处理音量调节,播放切换,WakeLock,音频焦点等事件
     */
    private static final class MusicPlayerHandler extends Handler {
        private float mCurrentVolume = 1.0f;
        private final WeakReference<MusicService> mService;

        MusicPlayerHandler(final MusicService service, final Looper looper) {
            super(looper);
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(final Message msg) {
            final MusicService service = mService.get();
            if (service == null) return;

            synchronized (service) {
                switch (msg.what) {
                    case MEDIA_PLAYER_ACTION_FADE_DOWN:
                        mCurrentVolume -= 0.05f;
                        if (mCurrentVolume > 0.2f) {
                            sendEmptyMessageDelayed(MEDIA_PLAYER_ACTION_FADE_DOWN, 10);
                        } else {
                            mCurrentVolume = 0.2f;
                        }
                        service.mPlayer.setVolume(mCurrentVolume);
                        break;
                    case MEDIA_PLAYER_ACTION_FADE_UP:
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(MEDIA_PLAYER_ACTION_FADE_UP, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        service.mPlayer.setVolume(mCurrentVolume);
                        break;
                    case MEDIA_PLAYER_ACTION_SERVER_DIED:
                        break;
                    case MEDIA_PLAYER_ACTION_WENT_TO_NEXT:
                        if (service.mCursor != null) {
                            service.mCursor.close();
                            service.mCursor = null;
                        }
                        break;
                    case MEDIA_PLAYER_ACTION_ENDED:
                        break;
                    case MEDIA_PLAYER_ACTION_RELEASE_WAKELOCK:
                        break;
                    case MEDIA_PLAYER_ACTION_FOCUS_CHANGED:
                        Log.d(TAG, "Received audio focus change event " + msg.arg1);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static final class MultiPlayer implements MediaPlayer.OnErrorListener,
            MediaPlayer.OnCompletionListener {
        private boolean mIsInitialized = false;

        private Handler mHandler;
        private MediaPlayer mNextMediaPlayer;
        private final WeakReference<MusicService> mService;
        private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();

        MultiPlayer(final MusicService service) {
            mService = new WeakReference<>(service);
            mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        }

        void setDataSource(final String path) {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
            if (mIsInitialized) setNextDataSource(null);
        }

        private boolean setDataSourceImpl(final MediaPlayer player, final String path) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(mService.get(), Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);

                player.prepare();
            } catch (final IOException todo) {
                return false;
            } catch (final IllegalArgumentException todo) {
                return false;
            }
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            return true;
        }

        void setNextDataSource(final String path) {
            try {
                mCurrentMediaPlayer.setNextMediaPlayer(null);
            } catch (IllegalArgumentException e) {
                Log.i(TAG, "Next media player is current one, continuing");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Media player not initialized!");
                return;
            }
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
            if (path == null) {
                return;
            }
            mNextMediaPlayer = new MediaPlayer();
            mNextMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
            mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
            if (setDataSourceImpl(mNextMediaPlayer, path)) {
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } else {
                if (mNextMediaPlayer != null) {
                    mNextMediaPlayer.release();
                    mNextMediaPlayer = null;
                }
            }
        }

        void setHandler(final Handler handler) {
            mHandler = handler;
        }

        boolean isInitialized() {
            return mIsInitialized;
        }

        void start() {
            mCurrentMediaPlayer.start();
        }

        void stop() {
            mCurrentMediaPlayer.reset();
            mIsInitialized = false;
        }

        void release() {
            mCurrentMediaPlayer.release();
        }

        void pause() {
            mCurrentMediaPlayer.pause();
        }

        void setVolume(final float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
        }

        int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }

        @Override
        public boolean onError(final MediaPlayer mp, final int what, final int extra) {
            Log.w(TAG, "Music Server Error what: " + what + " extra: " + extra);
            return false;
        }

        @Override
        public void onCompletion(final MediaPlayer mp) {
            if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = mNextMediaPlayer;
                mNextMediaPlayer = null;
                mHandler.sendEmptyMessage(MEDIA_PLAYER_ACTION_WENT_TO_NEXT);
            } else {
                mHandler.sendEmptyMessage(MEDIA_PLAYER_ACTION_ENDED);
                mHandler.sendEmptyMessage(MEDIA_PLAYER_ACTION_RELEASE_WAKELOCK);
            }
        }
    }

    private static final class ServiceStub extends IMusicServiceAPI.Stub {
        private final WeakReference<MusicService> mService;

        private ServiceStub(MusicService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void stop() throws RemoteException {
            if (mService.get() == null) return;
            mService.get().stop();
        }

        @Override
        public void pause() throws RemoteException {
            if (mService.get() == null) return;
            mService.get().pause();
        }

        @Override
        public void open(long[] list, int position, long sourceId) throws RemoteException {
            if (mService.get() == null) return;
            mService.get().open(list, position, sourceId);
        }

        @Override
        public void play() throws RemoteException {
            if (mService.get() == null) return;
            mService.get().play();
        }
    }
}

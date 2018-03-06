package com.andova.app.ui.music.player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.andova.app.ui.music.IMusicServiceAPI;

import java.lang.ref.WeakReference;

import static com.andova.app.ui.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_FOCUS_CHANGED;

/**
 * Created by Administrator on 2018-02-24.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MusicService extends Service {
    public static final String BROADCAST_ACTION_PREVIOUS = "com.andova.app.ui.music.player.MusicService.PREVIOUS";
    public static final String BROADCAST_ACTION_TOGGLE = "com.andova.app.ui.music.player.MusicService.TOGGLE";
    public static final String BROADCAST_ACTION_NEXT = "com.andova.app.ui.music.player.MusicService.NEXT";

    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;

    private final IBinder mBinder = new ServiceStub(this);
    private MediaDataSource mDataSource = new MediaDataSource();

    private MusicPlayer mMusicPlayer;
    private MediaTracker mMediaTracker;
    private AudioManager mAudioManager;
    private HandlerThread mHandlerThread;
    private MusicPlayerHandler mPlayerHandler;
    private MusicNotification mMusicNotification;
    private NotificationManagerCompat mNotificationManager;

    /**
     * 监听,转发给mPlayerHandler处理
     */
    private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            mPlayerHandler.obtainMessage(MEDIA_PLAYER_CODE_FOCUS_CHANGED, focusChange, 0).sendToTarget();
        }
    };

    @Override
    public IBinder onBind(final Intent intent) {
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        System.out.println("Service unbound");
        mServiceInUse = false;

        if (isPlaying()) return true;
        stopSelf(mServiceStartId);
        return true;
    }

    @Override
    public void onRebind(final Intent intent) {
        mServiceInUse = true;
    }

    @Override
    public void onCreate() {
        System.out.println("Creating service");
        super.onCreate();

        mNotificationManager = NotificationManagerCompat.from(this);
        mMusicNotification = new MusicNotification(this);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mMediaTracker = new MediaTracker(this); // 当Player接收到播放状态相关的回调时,发送信息给Handler处理
        mMediaTracker.setHandler(mPlayerHandler);

        mMusicPlayer = new MusicPlayer(this, mMediaTracker, mDataSource);

        mHandlerThread = new HandlerThread("MusicPlayerHandler", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mPlayerHandler = new MusicPlayerHandler(mMusicPlayer, mHandlerThread.getLooper()); // 处理播放状态相关
    }

    @Override
    public void onDestroy() {
        System.out.println("Destroying service");
        super.onDestroy();

        mPlayerHandler.removeCallbacksAndMessages(null);

        mMediaTracker.release();
        mMediaTracker = null;

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mDataSource.closeCursor();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        System.out.println("Got new intent " + intent + ", startId = " + startId);
        mServiceStartId = startId;

        if (intent != null) handleCommandIntent(intent);

        return START_NOT_STICKY;
    }

    /**
     * 处理Notification触发的媒体按钮点击事件
     */
    private void handleCommandIntent(Intent intent) {
        final String action = intent.getAction();
        System.out.println("handleCommandIntent ->action:" + action);
        if (TextUtils.isEmpty(action)) return;
        switch (action) {
            case BROADCAST_ACTION_NEXT:
                next();
                break;
            case BROADCAST_ACTION_PREVIOUS:
                previous();
                break;
            case BROADCAST_ACTION_TOGGLE:
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
                break;
        }
    }

    /**
     * 设置播放列表
     *
     * @param position -1标志随机播放
     */
    public void open(final long[] list, final int position, long sourceId) {
        mMusicPlayer.open(this, list, position, sourceId);
    }

    public void stop() {
        mMusicPlayer.stop(this);
    }

    public void play() {
        if (mMusicPlayer.play(this, mAudioManager, mAudioFocusListener, mPlayerHandler))
            updateNotification();
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (mMusicPlayer.pause(this, mPlayerHandler)) updateNotification();
    }

    public void previous() {
        mMusicPlayer.previous(this, mAudioManager, mAudioFocusListener, mPlayerHandler);
        updateNotification();
    }

    public void next() {
        mMusicPlayer.next(this, mAudioManager, mAudioFocusListener, mPlayerHandler, true);
        updateNotification();
    }

    public void updateNotification() {
        mMusicNotification.updateNotification(mNotificationManager, mDataSource);
    }

    public boolean isPlaying() {
        return mMusicPlayer.isPlaying();
    }

    boolean recentlyPlayed() {
        return mMusicPlayer.recentlyPlayed();
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
        public void previous() throws RemoteException {
            if (mService.get() == null) return;
            mService.get().previous();
        }

        @Override
        public void next() throws RemoteException {
            if (mService.get() == null) return;
            mService.get().next();
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

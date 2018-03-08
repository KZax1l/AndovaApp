package com.andova.app.music.player;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

import static com.andova.app.AndovaApplication.LOGGER;

/**
 * Created by Administrator on 2018-02-27.
 * 处理音量调节,播放切换,WakeLock,音频焦点等事件
 *
 * @author kzaxil
 * @since 1.0.0
 */
class MusicPlayerHandler extends Handler {
    /**
     * {@link MediaPlayer}播放完毕且暂时没有下一首
     */
    static final int MEDIA_PLAYER_CODE_ENDED = 1;
    /**
     * {@link MediaPlayer}播放完毕切换到下一首
     */
    static final int MEDIA_PLAYER_CODE_WENT_TO_NEXT = 2;
    /**
     * 释放{@link android.os.PowerManager.WakeLock}
     */
    static final int MEDIA_PLAYER_CODE_RELEASE_WAKELOCK = 3;
    /**
     * {@link MediaPlayer}播放出错
     */
    public static final int MEDIA_PLAYER_CODE_SERVER_DIED = 4;
    /**
     * 音频焦点变化
     */
    static final int MEDIA_PLAYER_CODE_FOCUS_CHANGED = 5;
    /**
     * 降低音量
     */
    static final int MEDIA_PLAYER_CODE_FADE_DOWN = 6;
    /**
     * 增加音量
     */
    static final int MEDIA_PLAYER_CODE_FADE_UP = 7;

    private float mCurrentVolume = 1.0f;
    private final WeakReference<MusicPlayer> mWeakReferencePlayer;

    MusicPlayerHandler(final MusicPlayer player, final Looper looper) {
        super(looper);
        mWeakReferencePlayer = new WeakReference<>(player);
    }

    @Override
    public void handleMessage(final Message msg) {
        final MusicPlayer player = mWeakReferencePlayer.get();
        if (player == null) {
            LOGGER.info("MusicPlayer is null");
            return;
        }

        synchronized (player) {
            switch (msg.what) {
                case MEDIA_PLAYER_CODE_FADE_DOWN:
                    mCurrentVolume -= 0.05f;
                    if (mCurrentVolume > 0.2f) {
                        sendEmptyMessageDelayed(MEDIA_PLAYER_CODE_FADE_DOWN, 10);
                    } else {
                        mCurrentVolume = 0.2f;
                    }
                    player.setVolume(mCurrentVolume);
                    break;
                case MEDIA_PLAYER_CODE_FADE_UP:
                    mCurrentVolume += 0.01f;
                    if (mCurrentVolume < 1.0f) {
                        sendEmptyMessageDelayed(MEDIA_PLAYER_CODE_FADE_UP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    player.setVolume(mCurrentVolume);
                    break;
                case MEDIA_PLAYER_CODE_SERVER_DIED:
                    break;
                case MEDIA_PLAYER_CODE_WENT_TO_NEXT:
                    player.goToNext();
                    break;
                case MEDIA_PLAYER_CODE_ENDED:
                    break;
                case MEDIA_PLAYER_CODE_RELEASE_WAKELOCK:
                    break;
                case MEDIA_PLAYER_CODE_FOCUS_CHANGED:
                    LOGGER.info("Received audio focus change event " + msg.arg1);
                    break;
                default:
                    break;
            }
        }
    }
}

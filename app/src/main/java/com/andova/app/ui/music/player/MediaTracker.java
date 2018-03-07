package com.andova.app.ui.music.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;

import java.io.IOException;
import java.lang.ref.WeakReference;

import static com.andova.app.AndovaApplication.LOGGER;
import static com.andova.app.ui.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_ENDED;
import static com.andova.app.ui.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_RELEASE_WAKELOCK;
import static com.andova.app.ui.music.player.MusicPlayerHandler.MEDIA_PLAYER_CODE_WENT_TO_NEXT;

/**
 * Created by Administrator on 2018-02-27.
 *
 * @author kzaxil
 * @since 1.0.0
 */
class MediaTracker implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private boolean mIsInitialized = false;

    private Handler mHandler;
    private MediaPlayer mNextMediaPlayer;
    private final WeakReference<Context> mContextWeakReference;
    private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();

    MediaTracker(final Context context) {
        mContextWeakReference = new WeakReference<>(context);
        if (mContextWeakReference.get() != null)
            mCurrentMediaPlayer.setWakeMode(mContextWeakReference.get(), PowerManager.PARTIAL_WAKE_LOCK);
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
                if (mContextWeakReference.get() != null)
                    player.setDataSource(mContextWeakReference.get(), Uri.parse(path));
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
            LOGGER.info("Next media player is current one, continuing");
        } catch (IllegalStateException e) {
            LOGGER.info("Media player not initialized!");
            return;
        }
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release();
            mNextMediaPlayer = null;
        }
        if (path == null) {
            LOGGER.info("Want to set next data source,but the path is null");
            return;
        }
        mNextMediaPlayer = new MediaPlayer();
        if (mContextWeakReference.get() != null)
            mNextMediaPlayer.setWakeMode(mContextWeakReference.get(), PowerManager.PARTIAL_WAKE_LOCK);
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

    private int getAudioSessionId() {
        return mCurrentMediaPlayer.getAudioSessionId();
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        LOGGER.info("Music Server Error what: " + what + " extra: " + extra);
        return false;
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        LOGGER.info("Media Player Completion");
        if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = mNextMediaPlayer;
            mNextMediaPlayer = null;
            mHandler.sendEmptyMessage(MEDIA_PLAYER_CODE_WENT_TO_NEXT);
        } else {
            mHandler.sendEmptyMessage(MEDIA_PLAYER_CODE_ENDED);
            mHandler.sendEmptyMessage(MEDIA_PLAYER_CODE_RELEASE_WAKELOCK);
        }
    }
}

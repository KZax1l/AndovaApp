package com.andova.app.ui.music.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;

import com.andova.app.Constants;
import com.andova.app.R;
import com.andova.app.ui.music.MusicActivity;
import com.andova.app.util.MusicUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.lang.ref.WeakReference;

import static com.andova.app.ui.music.player.MusicService.BROADCAST_ACTION_NEXT;
import static com.andova.app.ui.music.player.MusicService.BROADCAST_ACTION_PREVIOUS;
import static com.andova.app.ui.music.player.MusicService.BROADCAST_ACTION_TOGGLE;

/**
 * Created by Administrator on 2018-02-27.
 *
 * @author kzaxil
 * @since 1.0.0
 */
class MusicNotification {
    private static final int NOTIFY_MODE_NONE = 0;
    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;
    private int mNotifyMode = NOTIFY_MODE_NONE;
    /**
     * 开始播放歌曲时，记录当前时间戳
     */
    private long mNotificationPostTime = 0;
    private WeakReference<MusicService> mWeakRefService;

    MusicNotification(@NonNull MusicService weakReference) {
        mWeakRefService = new WeakReference<>(weakReference);
    }

    /**
     * 更新状态栏通知
     */
    void updateNotification(NotificationManagerCompat notificationManager, MediaDataSource dataSource) {
        final MusicService service = mWeakRefService.get();
        if (service == null) return;
        final int newNotifyMode;
        if (service.isPlaying()) {
            newNotifyMode = NOTIFY_MODE_FOREGROUND;
        } else if (service.recentlyPlayed()) {
            newNotifyMode = NOTIFY_MODE_BACKGROUND;
        } else {
            newNotifyMode = NOTIFY_MODE_NONE;
        }
        System.out.println("old notify mode:" + mNotifyMode + ",new notify mode:" + newNotifyMode);

        int notificationId = hashCode();
        if (mNotifyMode != newNotifyMode) {
            if (mNotifyMode == NOTIFY_MODE_FOREGROUND) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    service.stopForeground(newNotifyMode == NOTIFY_MODE_NONE);
                else
                    service.stopForeground(newNotifyMode == NOTIFY_MODE_NONE || newNotifyMode == NOTIFY_MODE_BACKGROUND);
            } else if (newNotifyMode == NOTIFY_MODE_NONE) {
                notificationManager.cancel(notificationId);
                mNotificationPostTime = 0;
            }
        }

        if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
            service.startForeground(notificationId, buildNotification(dataSource));
        } else if (newNotifyMode == NOTIFY_MODE_BACKGROUND) {
            notificationManager.notify(notificationId, buildNotification(dataSource));
        }

        mNotifyMode = newNotifyMode;
    }

    /**
     * 构建音乐播放器通知栏
     */
    private Notification buildNotification(MediaDataSource dataSource) {
        final MusicService service = mWeakRefService.get();
        if (service == null) return null;
        final String albumName = dataSource.getAlbumName();
        final String artistName = dataSource.getArtistName();
        String text = TextUtils.isEmpty(albumName) ? artistName : artistName + " - " + albumName;

        Intent nowPlayingIntent = new Intent(service, MusicActivity.class);
        nowPlayingIntent.setAction(Constants.NAVIGATE_ACTION_MUSIC);
        PendingIntent clickIntent = PendingIntent.getActivity(service, 0, nowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap artwork = ImageLoader.getInstance().loadImageSync(MusicUtil.getAlbumArtUri(dataSource.getAlbumId()).toString());

        if (artwork == null) {
            artwork = ImageLoader.getInstance().loadImageSync("drawable://" + R.mipmap.ic_album_default);
        }

        if (mNotificationPostTime == 0) {
            mNotificationPostTime = System.currentTimeMillis();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service)
                .setSmallIcon(R.mipmap.ic_music_note_white_48dp)
                .setLargeIcon(artwork)
                .setContentIntent(clickIntent)
                .setContentTitle(dataSource.getTrackName())
                .setContentText(text)
                .setWhen(mNotificationPostTime)
                .addAction(R.mipmap.ic_skip_previous_white_24dp,
                        "",
                        retrievePlaybackAction(BROADCAST_ACTION_PREVIOUS))
                .addAction(service.isPlaying() ? R.mipmap.ic_pause_white_24dp : R.mipmap.ic_play_arrow_white_24dp,
                        "",
                        retrievePlaybackAction(BROADCAST_ACTION_TOGGLE))
                .addAction(R.mipmap.ic_skip_next_white_24dp,
                        "",
                        retrievePlaybackAction(BROADCAST_ACTION_NEXT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) builder.setShowWhen(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            android.support.v4.media.app.NotificationCompat.MediaStyle style =
                    new android.support.v4.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2, 3);
            builder.setStyle(style);
        }
        if (artwork != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Palette.from(artwork).generate().getMutedColor(ContextCompat.getColor(service, R.color.colorToolbar)));
        }

        return builder.build();
    }

    /**
     * 点击了UI上的媒体按键,发送事件给MusicService
     */
    private PendingIntent retrievePlaybackAction(final String action) {
        final MusicService service = mWeakRefService.get();
        if (service == null) return null;
        final ComponentName serviceName = new ComponentName(service, MusicService.class);
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);

        /*
        这里也可以直接通过广播来实现—— PendingIntent.getBroadcast(this, 0, new Intent(action), 0);
         */
        return PendingIntent.getService(service, 0, intent, 0);
    }
}

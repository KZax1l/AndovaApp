package com.andova.app.music.player;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by Administrator on 2018-02-27.
 *
 * @author kzaxil
 * @since 1.0.0
 */
class MediaDataSource {
    private Cursor mAudioCursor;
    private Cursor mAlbumCursor;

    private static final String[] AUDIO_PROJECTION = new String[]{ //歌曲信息
            "audio._id AS _id", MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };
    private static final String[] ALBUM_PROJECTION = new String[]{ //专辑信息
            MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR
    };

    void updateCursor(Context context, final long trackId) {
        updateCursor(context, "_id=" + trackId, null);
    }

    /**
     * 更新播放曲目和专辑的相关信息
     */
    void updateCursor(Context context, final String selection, final String[] selectionArgs) {
        synchronized (this) {
            closeCursor(); //关闭mCursor和mAlbumCursor
            mAudioCursor = openCursorAndGoToFirst(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    AUDIO_PROJECTION, selection, selectionArgs);
        }
        updateAlbumCursor(context);
    }

    void updateCursor(Context context, final Uri uri) {
        synchronized (this) {
            closeCursor();
            mAudioCursor = openCursorAndGoToFirst(context, uri, AUDIO_PROJECTION, null, null);
        }
        updateAlbumCursor(context);
    }

    private void updateAlbumCursor(Context context) {
        long albumId = getAlbumId();
        if (albumId >= 0) {
            mAlbumCursor = openCursorAndGoToFirst(context, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    ALBUM_PROJECTION, "_id=" + albumId, null);
        } else {
            mAlbumCursor = null;
        }
    }

    private Cursor openCursorAndGoToFirst(Context context, Uri uri, String[] projection,
                                          String selection, String[] selectionArgs) {
        Cursor c = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    synchronized void closeCursor() {
        if (mAudioCursor != null) {
            mAudioCursor.close();
            mAudioCursor = null;
        }
        if (mAlbumCursor != null) {
            mAlbumCursor.close();
            mAlbumCursor = null;
        }
    }

    long getAlbumId() {
        synchronized (this) {
            if (mAudioCursor == null) return -1;
            return mAudioCursor.getLong(mAudioCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
        }
    }

    String getAlbumName() {
        synchronized (this) {
            if (mAudioCursor == null) return null;
            return mAudioCursor.getString(mAudioCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
        }
    }

    String getArtistName() {
        synchronized (this) {
            if (mAudioCursor == null) return null;
            return mAudioCursor.getString(mAudioCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
        }
    }

    String getTrackName() {
        synchronized (this) {
            if (mAudioCursor == null) return null;
            return mAudioCursor.getString(mAudioCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
        }
    }

    Cursor getAudioCursor() {
        return mAudioCursor;
    }

    public Cursor getAlbumCursor() {
        return mAlbumCursor;
    }
}

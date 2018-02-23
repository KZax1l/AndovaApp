package com.andova.app.ui.music.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.andova.app.ui.music.model.Song;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class SongLoader {
    private static Observable<List<Song>> getSongsForCursor(final Cursor cursor) {
        return Observable.create(new Observable.OnSubscribe<List<Song>>() {
            @Override
            public void call(Subscriber<? super List<Song>> subscriber) {
                List<Song> arrayList = new ArrayList<>();
                if ((cursor != null) && (cursor.moveToFirst()))
                    do {
                        long id = cursor.getLong(0);
                        String title = cursor.getString(1);
                        String artist = cursor.getString(2);
                        String album = cursor.getString(3);
                        int duration = cursor.getInt(4);
                        int trackNumber = cursor.getInt(5);
                        long artistId = cursor.getInt(6);
                        long albumId = cursor.getLong(7);
                        String path = cursor.getString(8);
                        arrayList.add(new Song(id, albumId, artistId, title, artist, album, duration, trackNumber, path));
                    }
                    while (cursor.moveToNext());
                if (cursor != null) {
                    cursor.close();
                }
                subscriber.onNext(arrayList);
                subscriber.onCompleted();
            }
        });
    }

    public static Observable<List<Song>> getAllSongs(Context context) {
        return getSongsForCursor(makeSongCursor(context, null, null));
    }

    public static Observable<List<Song>> searchSongs(Context context, String searchString) {
        return getSongsForCursor(makeSongCursor(context, "title LIKE ? or artist LIKE ? or album LIKE ? ",
                new String[]{"%" + searchString + "%", "%" + searchString + "%", "%" + searchString + "%"}));
    }

    public static Observable<List<Song>> getSongListInFolder(Context context, String path) {
        String[] whereArgs = new String[]{path + "%"};
        return getSongsForCursor(makeSongCursor(context, MediaStore.Audio.Media.DATA + " LIKE ?", whereArgs, null));
    }

    private static Cursor makeSongCursor(Context context, String selection, String[] paramArrayOfString) {
        return makeSongCursor(context, selection, paramArrayOfString, "");
    }

    private static Cursor makeSongCursor(Context context, String selection, String[] paramArrayOfString, String sortOrder) {
        String selectionStatement = "is_music=1 AND title != ''";

        if (!TextUtils.isEmpty(selection)) {
            selectionStatement = selectionStatement + " AND " + selection;
        }
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{"_id", "title", "artist", "album", "duration", "track", "artist_id", "album_id", MediaStore.Audio.Media.DATA},
                selectionStatement, paramArrayOfString, sortOrder);

    }
}

package com.andova.app.util;

import android.content.ContentUris;
import android.net.Uri;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MusicUtil {
    public static Uri getAlbumArtUri(long paramInt) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), paramInt);
    }
}

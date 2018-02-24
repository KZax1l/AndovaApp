package com.andova.app.ui.music.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2018-02-24.
 * <p>This is used by the music playback service to track the music tracks it is playing
 * It has extra meta data to determine where the track came from so that we can show the appropriate
 * song playing indicator</p>
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MusicPlaybackTrack implements Parcelable {
    public long mId;
    public long mSourceId;
    public int mSourcePosition;

    public static final Creator<MusicPlaybackTrack> CREATOR = new Creator<MusicPlaybackTrack>() {
        @Override
        public MusicPlaybackTrack createFromParcel(Parcel in) {
            return new MusicPlaybackTrack(in);
        }

        @Override
        public MusicPlaybackTrack[] newArray(int size) {
            return new MusicPlaybackTrack[size];
        }
    };

    public MusicPlaybackTrack(Parcel in) {
        mId = in.readLong();
        mSourceId = in.readLong();
        mSourcePosition = in.readInt();
    }

    public MusicPlaybackTrack(long id, long sourceId, int sourcePosition) {
        mId = id;
        mSourceId = sourceId;
        mSourcePosition = sourcePosition;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeLong(mSourceId);
        dest.writeInt(mSourcePosition);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MusicPlaybackTrack) {
            MusicPlaybackTrack other = (MusicPlaybackTrack) o;
            return mId == other.mId
                    && mSourceId == other.mSourceId
                    && mSourcePosition == other.mSourcePosition;
        }
        return super.equals(o);
    }
}

package com.andova.app.music.model;

/**
 * Created by Administrator on 2018-02-23.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class Song {
    public final long id;
    public final String title;
    public final int duration;
    public final long albumId;
    public final long artistId;
    public final int trackNumber;
    public final String path;
    public final String albumName;
    public final String artistName;

    public float playCountScore;

    public Song() {
        this.id = -1;
        this.albumId = -1;
        this.artistId = -1;
        this.title = "";
        this.artistName = "";
        this.albumName = "";
        this.duration = -1;
        this.trackNumber = -1;
        this.path = "";
    }

    public Song(long _id, long _albumId, long _artistId, String _title, String _artistName,
                String _albumName, int _duration, int _trackNumber) {
        this.id = _id;
        this.albumId = _albumId;
        this.artistId = _artistId;
        this.title = _title;
        this.artistName = _artistName;
        this.albumName = _albumName;
        this.duration = _duration;
        this.trackNumber = _trackNumber;
        this.path = "";
    }

    public Song(long _id, long _albumId, long _artistId, String _title, String _artistName,
                String _albumName, int _duration, int _trackNumber, String _path) {
        this.id = _id;
        this.albumId = _albumId;
        this.artistId = _artistId;
        this.title = _title;
        this.artistName = _artistName;
        this.albumName = _albumName;
        this.duration = _duration;
        this.trackNumber = _trackNumber;
        this.path = _path;
    }

    public void setPlayCountScore(float playCountScore) {
        this.playCountScore = playCountScore;
    }

    public float getPlayCountScore() {
        return playCountScore;
    }
}

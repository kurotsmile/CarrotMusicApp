package com.carrot.heartbeatmusic;

import org.json.JSONObject;

public class Song {
    public final String id;
    public final String name;
    public final String artist;
    public final String album;
    public final String genre;
    public final String year;
    public final String avatar;
    public final String mp3;
    public final String url;
    public final String description;

    public Song(JSONObject json) {
        id = json.optString("id");
        name = json.optString("name");
        artist = json.optString("artist");
        album = json.optString("album");
        genre = json.optString("genre");
        year = json.optString("year");
        avatar = json.optString("avatar");
        mp3 = json.optString("mp3");
        url = json.optString("url");
        description = json.optString("description");
    }

    public boolean canPlay() {
        return mp3 != null && !mp3.trim().isEmpty();
    }
}

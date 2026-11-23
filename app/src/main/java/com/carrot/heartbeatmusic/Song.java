package com.carrot.heartbeatmusic;

import org.json.JSONObject;

public class Song {
    public final String id;
    public final String name;
    public final String artist;
    public final String artistId;
    public final String artistIds;
    public final String album;
    public final String genre;
    public final String year;
    public final String lang;
    public final String country;
    public final String avatar;
    public final String mp3;
    public final String url;
    public final String description;
    public final String lyrics;
    public final int viewCount;

    public Song(JSONObject json) {
        id = json.optString("id");
        name = json.optString("name");
        artist = json.optString("artist");
        artistId = json.optString("artist_id");
        artistIds = json.optString("artist_ids", artistId);
        album = json.optString("album");
        genre = json.optString("genre");
        year = json.optString("year");
        lang = json.optString("lang");
        country = json.optString("country");
        avatar = json.optString("avatar");
        mp3 = json.optString("mp3");
        url = json.optString("url");
        description = json.optString("description");
        lyrics = json.optString("lyrics");
        viewCount = json.optInt("view_count", 0);
    }

    public boolean canPlay() {
        return mp3 != null && !mp3.trim().isEmpty();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("artist", artist);
            json.put("artist_id", artistId);
            json.put("artist_ids", artistIds);
            json.put("album", album);
            json.put("genre", genre);
            json.put("year", year);
            json.put("lang", lang);
            json.put("country", country);
            json.put("avatar", avatar);
            json.put("mp3", mp3);
            json.put("url", url);
            json.put("description", description);
            json.put("lyrics", lyrics);
            json.put("view_count", viewCount);
        } catch (Exception ignored) {
        }
        return json;
    }
}

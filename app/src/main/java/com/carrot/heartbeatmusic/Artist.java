package com.carrot.heartbeatmusic;

import org.json.JSONObject;

public class Artist {
    public final String id;
    public final String name;
    public final String lang;
    public final String avatar;
    public final String description;
    public final String url;
    public final int songCount;

    public Artist(JSONObject json) {
        id = json.optString("id");
        name = json.optString("name");
        lang = json.optString("lang");
        avatar = json.optString("avatar");
        description = json.optString("description");
        url = json.optString("url");
        songCount = json.optInt("song_count", 0);
    }
}

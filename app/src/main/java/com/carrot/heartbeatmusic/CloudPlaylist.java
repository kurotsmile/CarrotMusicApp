package com.carrot.heartbeatmusic;

import org.json.JSONArray;
import org.json.JSONObject;

public class CloudPlaylist {
    public final int id;
    public final String name;
    public final String description;
    public final int songCount;

    public CloudPlaylist(JSONObject json) {
        id = json.optInt("id", 0);
        name = json.optString("name");
        description = json.optString("description");
        JSONArray songs = json.optJSONArray("songs");
        songCount = songs == null ? 0 : songs.length();
    }
}

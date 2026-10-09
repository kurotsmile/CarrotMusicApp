package com.carrot.heartbeatmusic;

import org.json.JSONObject;

public class MenuListItem {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String avatar;
    public final String type;

    public MenuListItem(JSONObject json) {
        id = json.optString("id");
        title = json.optString("title");
        subtitle = json.optString("subtitle");
        avatar = json.optString("avatar");
        type = json.optString("type");
    }
}

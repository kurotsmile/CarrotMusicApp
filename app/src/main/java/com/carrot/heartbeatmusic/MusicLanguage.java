package com.carrot.heartbeatmusic;

import org.json.JSONObject;

public class MusicLanguage {
    public final String name;
    public final String lang;
    public final String country;
    public final String icon;

    public MusicLanguage(JSONObject json) {
        name = json.optString("name");
        lang = json.optString("lang");
        country = json.optString("country");
        icon = json.optString("icon");
    }

    public String label() {
        String code = lang == null || lang.isEmpty() ? country : lang;
        if (name == null || name.isEmpty()) {
            return code == null ? "" : code.toUpperCase();
        }
        return code == null || code.isEmpty() ? name : name + " · " + code.toUpperCase();
    }
}

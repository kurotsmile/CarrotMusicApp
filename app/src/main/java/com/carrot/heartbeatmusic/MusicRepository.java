package com.carrot.heartbeatmusic;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MusicRepository {
    public interface Callback {
        void onSongs(List<Song> songs);
        void onError(String message);
    }

    public void loadHome(Callback callback) {
        load(BuildConfig.MUSIC_API_BASE + "?action=home&limit=36", "songs", callback);
    }

    public void search(String query, Callback callback) {
        String encoded;
        try {
            encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            encoded = query;
        }
        load(BuildConfig.MUSIC_API_BASE + "?action=search&limit=48&q=" + encoded, "songs", callback);
    }

    private void load(String url, String arrayKey, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(16000);
                connection.setRequestProperty("Accept", "application/json");
                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                String body = readAll(stream);
                JSONObject root = new JSONObject(body);
                if (!root.optBoolean("ok", false)) {
                    callback.onError(root.optString("error", "Can not load music"));
                    return;
                }
                JSONArray items = root.optJSONArray(arrayKey);
                if (items == null) {
                    items = new JSONArray();
                }
                List<Song> songs = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    Song song = new Song(items.getJSONObject(i));
                    if (song.canPlay()) {
                        songs.add(song);
                    }
                }
                callback.onSongs(songs);
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? "Can not load music" : e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}

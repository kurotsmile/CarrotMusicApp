package com.carrot.heartbeatmusic;

import android.content.Context;
import android.content.SharedPreferences;

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
    private static final long SONG_LIST_TTL_MS = 24L * 60L * 60L * 1000L;
    private static final long LANGUAGE_TTL_MS = 7L * 24L * 60L * 60L * 1000L;
    private final SharedPreferences cache;

    public MusicRepository(Context context) {
        cache = context.getSharedPreferences("heartbeat_music_cache", Context.MODE_PRIVATE);
    }

    public interface Callback {
        void onSongs(List<Song> songs);
        void onError(String message);
    }

    public interface LanguageCallback {
        void onLanguages(List<MusicLanguage> languages);
        void onError(String message);
    }

    public void loadHome(String lang, Callback callback) {
        String url = BuildConfig.MUSIC_API_BASE + "?action=home&limit=36&lang=" + encode(lang);
        load(url, "songs", "songs_home_v2_" + (lang == null || lang.isEmpty() ? "en" : lang), SONG_LIST_TTL_MS, callback);
    }

    public void search(String query, String lang, Callback callback) {
        load(BuildConfig.MUSIC_API_BASE + "?action=search&limit=48&q=" + encode(query) + "&lang=" + encode(lang), "songs", "", 0, callback);
    }

    public void loadLanguages(LanguageCallback callback) {
        String cached = getCachedBody("languages_v2", LANGUAGE_TTL_MS);
        if (cached != null) {
            try {
                callback.onLanguages(parseLanguages(cached));
                return;
            } catch (Exception ignored) {
            }
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(BuildConfig.MUSIC_API_BASE + "?action=languages").openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(16000);
                connection.setRequestProperty("Accept", "application/json");
                String body = readAll(connection.getInputStream());
                List<MusicLanguage> languages = parseLanguages(body);
                saveCachedBody("languages_v2", body);
                callback.onLanguages(languages);
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? "Can not load languages" : e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String encode(String value) {
        String encoded;
        try {
            encoded = URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            encoded = value == null ? "" : value;
        }
        return encoded;
    }

    private void load(String url, String arrayKey, String cacheKey, long ttlMs, Callback callback) {
        String cached = getCachedBody(cacheKey, ttlMs);
        if (cached != null) {
            try {
                callback.onSongs(parseSongs(cached, arrayKey));
                return;
            } catch (Exception ignored) {
            }
        }

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
                if (cacheKey != null && !cacheKey.isEmpty() && ttlMs > 0) {
                    saveCachedBody(cacheKey, body);
                }
                callback.onSongs(parseSongs(body, arrayKey));
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? "Can not load music" : e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private List<Song> parseSongs(String body, String arrayKey) throws Exception {
        JSONObject root = new JSONObject(body);
        if (!root.optBoolean("ok", false)) {
            throw new IllegalStateException(root.optString("error", "Can not load music"));
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
        return songs;
    }

    private List<MusicLanguage> parseLanguages(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        if (!root.optBoolean("ok", false)) {
            throw new IllegalStateException(root.optString("error", "Can not load languages"));
        }
        JSONArray items = root.optJSONArray("languages");
        List<MusicLanguage> languages = new ArrayList<>();
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                MusicLanguage language = new MusicLanguage(items.getJSONObject(i));
                if (language.lang != null && !language.lang.trim().isEmpty()) {
                    languages.add(language);
                }
            }
        }
        return languages;
    }

    private String getCachedBody(String key, long ttlMs) {
        if (key == null || key.isEmpty() || ttlMs <= 0) {
            return null;
        }
        long savedAt = cache.getLong(key + "_saved_at", 0L);
        String body = cache.getString(key + "_body", null);
        if (body == null || savedAt <= 0L || System.currentTimeMillis() - savedAt > ttlMs) {
            return null;
        }
        return body;
    }

    private void saveCachedBody(String key, String body) {
        if (key == null || key.isEmpty() || body == null || body.isEmpty()) {
            return;
        }
        cache.edit()
                .putLong(key + "_saved_at", System.currentTimeMillis())
                .putString(key + "_body", body)
                .apply();
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

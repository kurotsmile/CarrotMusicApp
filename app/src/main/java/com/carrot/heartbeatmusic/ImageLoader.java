package com.carrot.heartbeatmusic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {
    public void load(String url, ImageView target) {
        target.setTag(url);
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                try (InputStream stream = connection.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    target.post(() -> {
                        if (url.equals(target.getTag()) && bitmap != null) {
                            target.setImageBitmap(bitmap);
                        }
                    });
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}

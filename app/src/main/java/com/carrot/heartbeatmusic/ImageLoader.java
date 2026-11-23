package com.carrot.heartbeatmusic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private final File cacheDirectory;
    private final LruCache<String, Bitmap> memoryCache;
    private final ConcurrentHashMap<String, Object> urlLocks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public ImageLoader(Context context) {
        cacheDirectory = new File(context.getApplicationContext().getFilesDir(), "avatar_cache");
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }
        int maxMemoryKb = (int) (Runtime.getRuntime().maxMemory() / 1024L);
        memoryCache = new LruCache<String, Bitmap>(Math.min(16 * 1024, maxMemoryKb / 8)) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return Math.max(1, bitmap.getByteCount() / 1024);
            }
        };
    }

    public void load(String url, ImageView target) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        target.setTag(url);
        Bitmap cached = memoryCache.get(url);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        executor.execute(() -> {
            Bitmap bitmap = loadBitmap(url);
            target.post(() -> {
                if (url.equals(target.getTag()) && bitmap != null) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }

    public void prefetch(String url) {
        if (url == null || url.trim().isEmpty() || memoryCache.get(url) != null) {
            return;
        }
        executor.execute(() -> loadBitmap(url));
    }

    public Uri cachedUri(String url) {
        File file = cacheFile(url);
        return file.isFile() && file.length() > 0L ? Uri.fromFile(file) : Uri.parse(url);
    }

    private Bitmap loadBitmap(String url) {
        Bitmap cached = memoryCache.get(url);
        if (cached != null) {
            return cached;
        }
        Object lock = urlLocks.computeIfAbsent(url, key -> new Object());
        synchronized (lock) {
            cached = memoryCache.get(url);
            if (cached != null) {
                return cached;
            }
            File file = cacheFile(url);
            if (!file.isFile() || file.length() == 0L) {
                if (!download(url, file)) {
                    return null;
                }
            }
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap == null) {
                file.delete();
                return null;
            }
            memoryCache.put(url, bitmap);
            return bitmap;
        }
    }

    private boolean download(String url, File destination) {
        HttpURLConnection connection = null;
        File temporary = new File(cacheDirectory, destination.getName() + ".tmp");
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return false;
            }
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(temporary, false)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
            if (temporary.length() == 0L) {
                temporary.delete();
                return false;
            }
            if (destination.exists()) {
                destination.delete();
            }
            if (!temporary.renameTo(destination)) {
                temporary.delete();
                return false;
            }
            return true;
        } catch (Exception ignored) {
            temporary.delete();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private File cacheFile(String url) {
        return new File(cacheDirectory, cacheKey(url) + ".img");
    }

    private String cacheKey(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8"));
            StringBuilder key = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                key.append(String.format("%02x", value & 0xff));
            }
            return key.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(url.hashCode());
        }
    }
}

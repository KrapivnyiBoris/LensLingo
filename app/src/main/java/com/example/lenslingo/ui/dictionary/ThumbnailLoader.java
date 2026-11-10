package com.example.lenslingo.ui.dictionary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ThumbnailLoader {

    private static final int SIZE = 160;
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 1024 / 16)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private ThumbnailLoader() {}

    public static void load(ImageView view, String uri) {
        view.setTag(uri);
        view.setImageDrawable(null);
        if (uri == null || !uri.startsWith("file:")) return;

        Bitmap cached = CACHE.get(uri);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }

        POOL.execute(() -> {
            Bitmap bmp = decode(uri);
            if (bmp == null) return;
            CACHE.put(uri, bmp);
            MAIN.post(() -> {
                if (uri.equals(view.getTag())) view.setImageBitmap(bmp);
            });
        });
    }

    private static Bitmap decode(String uri) {
        File file = new File(java.net.URI.create(uri));
        if (!file.exists()) return null;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), o);

        int sample = 1;
        while (o.outWidth / (sample * 2) >= SIZE && o.outHeight / (sample * 2) >= SIZE) {
            sample *= 2;
        }
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(file.getPath(), o);
    }
}
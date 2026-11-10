package com.example.lenslingo.data.local;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public final class PhotoStore {

    private static final int JPEG_QUALITY = 85;

    private PhotoStore() {}

    private static File dir(Context context) {
        File d = new File(context.getFilesDir(), "photos");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    // Блокуючий метод: викликати лише з фонового потоку. Повертає file:// URI або null
    public static String save(Context context, Bitmap bitmap) {
        File file = new File(dir(context), UUID.randomUUID() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            return file.toURI().toString();
        } catch (IOException e) {
            file.delete();
            return null;
        }
    }

    public static void delete(String uri) {
        if (uri == null || !uri.startsWith("file:")) return;
        try {
            new File(java.net.URI.create(uri)).delete();
        } catch (Exception ignored) {
        }
    }
}
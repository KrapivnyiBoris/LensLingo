package com.example.lenslingo.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final int MAX_DIMENSION = 1600;
    private static final int JPEG_QUALITY = 80;

    public static String uriToBase64(Context context, Uri uri) throws IOException {
        Bitmap bitmap = loadScaledBitmap(context, uri);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);

        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
    }

    private static Bitmap loadScaledBitmap(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        Bitmap original = BitmapFactory.decodeStream(input);
        if (input != null) input.close();

        if (original == null) {
            throw new IOException("Could not decode image");
        }

        int width = original.getWidth();
        int height = original.getHeight();
        float scale = Math.min(1f, (float) MAX_DIMENSION / Math.max(width, height));

        if (scale >= 1f) return original;

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }
}
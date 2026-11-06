package com.example.lenslingo.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final int MAX_DIMENSION = 1600;
    private static final int JPEG_QUALITY = 80;

    public static Bitmap loadUprightBitmap(Context context, Uri uri) throws IOException {
        int rotation = readRotation(context, uri);

        Bitmap original;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            original = BitmapFactory.decodeStream(input);
        }
        if (original == null) {
            throw new IOException("Could not decode image");
        }

        float scale = Math.min(1f,
                (float) MAX_DIMENSION / Math.max(original.getWidth(), original.getHeight()));
        if (scale == 1f && rotation == 0) {
            return original;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(original, 0, 0,
                original.getWidth(), original.getHeight(), matrix, true);
    }

    public static String toBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
    }

    private static int readRotation(Context context, Uri uri) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return 0;
            ExifInterface exif = new ExifInterface(input);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
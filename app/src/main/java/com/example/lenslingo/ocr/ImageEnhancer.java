package com.example.lenslingo.ocr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;

public final class ImageEnhancer {

    private static final float SCALE = 1.5f;
    private static final float CONTRAST = 1.5f;

    private ImageEnhancer() {}

    public static Bitmap enhance(Bitmap src) {
        int w = Math.round(src.getWidth() * SCALE);
        int h = Math.round(src.getHeight() * SCALE);
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        float shift = (1f - CONTRAST) * 128f;
        ColorMatrix matrix = new ColorMatrix(new float[]{
                CONTRAST, 0, 0, 0, shift,
                0, CONTRAST, 0, 0, shift,
                0, 0, CONTRAST, 0, shift,
                0, 0, 0, 1, 0});

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        new Canvas(out).drawBitmap(src, null, new Rect(0, 0, w, h), paint);
        return out;
    }
}
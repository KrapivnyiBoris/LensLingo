package com.example.lenslingo.ui.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ViewfinderOverlayView extends View {

    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public ViewfinderOverlayView(Context context) {
        this(context, null);
    }

    public ViewfinderOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        cornerPaint.setColor(0xFFFFFFFF);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(3 * density);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        gridPaint.setColor(0x55FFFFFF);
        gridPaint.setStrokeWidth(density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float l = 2 * density, t = 2 * density;
        float r = getWidth() - 2 * density, b = getHeight() - 2 * density;
        float len = 30 * density;

        for (int i = 1; i <= 2; i++) {
            float x = l + (r - l) * i / 3f;
            float y = t + (b - t) * i / 3f;
            canvas.drawLine(x, t, x, b, gridPaint);
            canvas.drawLine(l, y, r, y, gridPaint);
        }

        canvas.drawLine(l, t, l + len, t, cornerPaint);
        canvas.drawLine(l, t, l, t + len, cornerPaint);
        canvas.drawLine(r, t, r - len, t, cornerPaint);
        canvas.drawLine(r, t, r, t + len, cornerPaint);
        canvas.drawLine(l, b, l + len, b, cornerPaint);
        canvas.drawLine(l, b, l, b - len, cornerPaint);
        canvas.drawLine(r, b, r - len, b, cornerPaint);
        canvas.drawLine(r, b, r, b - len, cornerPaint);
    }
}
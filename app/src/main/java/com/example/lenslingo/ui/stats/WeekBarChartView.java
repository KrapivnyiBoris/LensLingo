package com.example.lenslingo.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

public class WeekBarChartView extends View {

    private String[] labels = new String[0];
    private int[] counts = new int[0];

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint();
    private final RectF rect = new RectF();
    private final float density;

    public WeekBarChartView(Context context) {
        this(context, null);
    }

    public WeekBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        barPaint.setColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
        int textColor = resolveColor(android.R.attr.textColorSecondary);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        linePaint.setColor(textColor);
        linePaint.setAlpha(80);
        linePaint.setStrokeWidth(density);
    }

    private int resolveColor(int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.resourceId != 0 ? ContextCompat.getColor(getContext(), tv.resourceId) : tv.data;
    }

    public void setData(String[] labels, int[] counts) {
        this.labels = labels;
        this.counts = counts;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = counts.length;
        if (n == 0) return;

        float labelH = 24 * density;
        float topPad = 20 * density;
        float chartBottom = getHeight() - labelH;
        float chartH = chartBottom - topPad;
        float slot = (float) getWidth() / n;
        float barW = slot * 0.55f;

        int max = 1;
        for (int c : counts) max = Math.max(max, c);

        canvas.drawLine(0, chartBottom, getWidth(), chartBottom, linePaint);

        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2f;
            float h = chartH * counts[i] / max;

            if (counts[i] > 0) {
                rect.set(cx - barW / 2f, chartBottom - h, cx + barW / 2f, chartBottom);
                canvas.drawRoundRect(rect, 4 * density, 4 * density, barPaint);
                canvas.drawText(String.valueOf(counts[i]), cx,
                        chartBottom - h - 4 * density, textPaint);
            }
            canvas.drawText(labels[i], cx, getHeight() - 6 * density, textPaint);
        }
    }
}
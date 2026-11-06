package com.example.lenslingo.ui.result;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.lenslingo.data.model.OcrWord;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxImageView extends View {

    public interface OnWordTapListener {
        void onWordTap(OcrWord word);
    }

    private Bitmap bitmap;
    private List<OcrWord> words = new ArrayList<>();
    private OcrWord selectedWord;
    private OnWordTapListener listener;

    private final RectF imageRect = new RectF();
    private final RectF tmpRect = new RectF();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float touchPadding;

    public BoundingBoxImageView(Context context) {
        super(context);
        init();
    }

    public BoundingBoxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        touchPadding = 6 * density;

        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(0xFF2196F3);
        boxPaint.setStrokeWidth(2 * density);

        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setColor(0xFF4CAF50);
        selectedPaint.setStrokeWidth(4 * density);
    }

    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate();
    }

    public void setWords(List<OcrWord> words) {
        this.words = words;
        this.selectedWord = null;
        invalidate();
    }

    public void setOnWordTapListener(OnWordTapListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;

        computeImageRect();
        canvas.drawBitmap(bitmap, null, imageRect, bitmapPaint);

        for (OcrWord word : words) {
            if (!fillViewRect(word, tmpRect)) continue;
            canvas.drawRect(tmpRect, word == selectedWord ? selectedPaint : boxPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                OcrWord hit = findWordAt(event.getX(), event.getY());
                if (hit != null) {
                    selectedWord = hit;
                    invalidate();
                    if (listener != null) listener.onWordTap(hit);
                }
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void computeImageRect() {
        float scale = Math.min(
                (float) getWidth() / bitmap.getWidth(),
                (float) getHeight() / bitmap.getHeight());
        float drawnWidth = bitmap.getWidth() * scale;
        float drawnHeight = bitmap.getHeight() * scale;
        float left = (getWidth() - drawnWidth) / 2f;
        float top = (getHeight() - drawnHeight) / 2f;
        imageRect.set(left, top, left + drawnWidth, top + drawnHeight);
    }

    private boolean fillViewRect(OcrWord word, RectF out) {
        int[] b = word.boundingBox;
        if (b == null || b.length != 4) return false;

        out.set(imageRect.left + b[1] / 1000f * imageRect.width(),
                imageRect.top + b[0] / 1000f * imageRect.height(),
                imageRect.left + b[3] / 1000f * imageRect.width(),
                imageRect.top + b[2] / 1000f * imageRect.height());
        return true;
    }

    private OcrWord findWordAt(float x, float y) {
        if (bitmap == null) return null;
        computeImageRect();

        OcrWord best = null;
        float bestArea = Float.MAX_VALUE;

        for (OcrWord word : words) {
            if (!fillViewRect(word, tmpRect)) continue;
            tmpRect.inset(-touchPadding, -touchPadding);
            if (tmpRect.contains(x, y)) {
                float area = tmpRect.width() * tmpRect.height();
                if (area < bestArea) {
                    bestArea = area;
                    best = word;
                }
            }
        }
        return best;
    }
}
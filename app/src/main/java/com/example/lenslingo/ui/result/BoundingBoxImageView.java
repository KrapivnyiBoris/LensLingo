package com.example.lenslingo.ui.result;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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

    private static final float DRAW_SCALE = 1.08f;
    private static final float HIT_SCALE = 1.3f;

    private Bitmap bitmap;
    private List<OcrWord> words = new ArrayList<>();
    private OcrWord selectedWord;
    private OnWordTapListener listener;

    private final RectF imageRect = new RectF();
    private final float[] quad = new float[8];
    private final Path path = new Path();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;

    public BoundingBoxImageView(Context context) {
        super(context);
        init();
    }

    public BoundingBoxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(0xFFFFFFFF);
        outlinePaint.setStrokeJoin(Paint.Join.ROUND);

        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(0xFF1976D2);
        boxPaint.setStrokeWidth(2 * density);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);

        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setColor(0xFF2E7D32);
        selectedPaint.setStrokeWidth(3.5f * density);
        selectedPaint.setStrokeJoin(Paint.Join.ROUND);
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
            if (word == selectedWord) continue;
            drawWord(canvas, word, boxPaint);
        }
        if (selectedWord != null) {
            drawWord(canvas, selectedWord, selectedPaint);
        }
    }

    private void drawWord(Canvas canvas, OcrWord word, Paint colorPaint) {
        if (!fillQuad(word, DRAW_SCALE)) return;

        path.reset();
        path.moveTo(quad[0], quad[1]);
        path.lineTo(quad[2], quad[3]);
        path.lineTo(quad[4], quad[5]);
        path.lineTo(quad[6], quad[7]);
        path.close();

        outlinePaint.setStrokeWidth(colorPaint.getStrokeWidth() + 3 * density);
        canvas.drawPath(path, outlinePaint);
        canvas.drawPath(path, colorPaint);
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

    private boolean fillQuad(OcrWord word, float scale) {
        float[] c = word.corners;
        if (c == null || c.length != 8) return false;

        float cx = 0;
        float cy = 0;
        for (int i = 0; i < 4; i++) {
            quad[i * 2] = imageRect.left + c[i * 2] * imageRect.width();
            quad[i * 2 + 1] = imageRect.top + c[i * 2 + 1] * imageRect.height();
            cx += quad[i * 2];
            cy += quad[i * 2 + 1];
        }
        cx /= 4;
        cy /= 4;

        for (int i = 0; i < 4; i++) {
            quad[i * 2] = cx + (quad[i * 2] - cx) * scale;
            quad[i * 2 + 1] = cy + (quad[i * 2 + 1] - cy) * scale;
        }
        return true;
    }

    private OcrWord findWordAt(float x, float y) {
        if (bitmap == null) return null;
        computeImageRect();

        OcrWord best = null;
        float bestArea = Float.MAX_VALUE;

        for (OcrWord word : words) {
            if (!fillQuad(word, HIT_SCALE)) continue;
            if (!pointInQuad(x, y)) continue;

            float area = quadArea();
            if (area < bestArea) {
                bestArea = area;
                best = word;
            }
        }
        return best;
    }

    private boolean pointInQuad(float x, float y) {
        boolean inside = false;
        for (int i = 0, j = 3; i < 4; j = i++) {
            float xi = quad[i * 2];
            float yi = quad[i * 2 + 1];
            float xj = quad[j * 2];
            float yj = quad[j * 2 + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    private float quadArea() {
        float sum = 0;
        for (int i = 0, j = 3; i < 4; j = i++) {
            sum += quad[j * 2] * quad[i * 2 + 1] - quad[i * 2] * quad[j * 2 + 1];
        }
        return Math.abs(sum) / 2f;
    }
}
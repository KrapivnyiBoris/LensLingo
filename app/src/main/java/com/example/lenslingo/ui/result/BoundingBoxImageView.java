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
import android.graphics.CornerPathEffect;

import com.example.lenslingo.data.model.OcrWord;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxImageView extends View {

    public interface OnWordTapListener {
        void onWordTap(OcrWord word);
    }

    public static boolean SOFT_STYLE = true;
    public static boolean DEBUG_LINES = false;
    private static final float PAD_LEFT_CHARS = 0f;
    private static final float PAD_RIGHT = 0f;
    private final Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final float DRAW_SCALE = 1.03f;
    private static final float HIT_SCALE = 1.3f;

    private static final int COLOR_WORD = 0xFF1976D2;
    private static final int COLOR_NAME = 0xFF8E24AA;
    private static final int COLOR_SELECTED = 0xFF2E7D32;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        boxPaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setColor(0xFFE53935);
        debugPaint.setStrokeWidth(1f * density);
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
            if (word != selectedWord) drawWord(canvas, word, false);
        }
        if (selectedWord != null) drawWord(canvas, selectedWord, true);

        if (DEBUG_LINES) {
            int last = -1;
            for (OcrWord word : words) {
                if (word.lineIndex == last || word.lineBox == null) continue;
                last = word.lineIndex;
                float[] b = word.lineBox;
                canvas.drawRect(
                        imageRect.left + b[0] * imageRect.width(),
                        imageRect.top + b[1] * imageRect.height(),
                        imageRect.left + b[2] * imageRect.width(),
                        imageRect.top + b[3] * imageRect.height(),
                        debugPaint);
            }
        }
    }

    private void drawWord(Canvas canvas, OcrWord word, boolean selected) {
        if (!fillQuad(word, DRAW_SCALE)) return;

        path.reset();
        path.moveTo(quad[0], quad[1]);
        path.lineTo(quad[2], quad[3]);
        path.lineTo(quad[4], quad[5]);
        path.lineTo(quad[6], quad[7]);
        path.close();

        int color = selected ? COLOR_SELECTED
                : (word.kind == OcrWord.KIND_NAME ? COLOR_NAME : COLOR_WORD);

        if (SOFT_STYLE) {
            fillPaint.setPathEffect(new CornerPathEffect(4 * density));
            fillPaint.setColor(color);
            fillPaint.setAlpha(selected ? 120 : 30);
            canvas.drawPath(path, fillPaint);
            fillPaint.setPathEffect(null);

            if (selected) {
                boxPaint.setPathEffect(new CornerPathEffect(4 * density));
                boxPaint.setColor(color);
                boxPaint.setStrokeWidth(2 * density);
                canvas.drawPath(path, boxPaint);
                boxPaint.setPathEffect(null);
            }
            return;
        }

        float stroke = (selected ? 2.5f : 1.2f) * density;
        fillPaint.setColor(color);
        fillPaint.setAlpha(selected ? 60 : 32);
        canvas.drawPath(path, fillPaint);
        outlinePaint.setStrokeWidth(stroke + 2 * density);
        canvas.drawPath(path, outlinePaint);
        boxPaint.setColor(color);
        boxPaint.setStrokeWidth(stroke);
        canvas.drawPath(path, boxPaint);
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

        float ux = quad[2] - quad[0];
        float uy = quad[3] - quad[1];
        float width = (float) Math.hypot(ux, uy);
        float height = (float) Math.hypot(quad[6] - quad[0], quad[7] - quad[1]);
        if (width > 0) {
            ux /= width;
            uy /= width;
            float charWidth = width / Math.max(1, word.text.length());
            float l = PAD_LEFT_CHARS * charWidth;
            float r = PAD_RIGHT * height;
            quad[0] -= ux * l; quad[1] -= uy * l;
            quad[6] -= ux * l; quad[7] -= uy * l;
            quad[2] += ux * r; quad[3] += uy * r;
            quad[4] += ux * r; quad[5] += uy * r;
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
package com.example.lenslingo.ocr;

import android.util.Log;

import com.example.lenslingo.data.model.OcrWord;

import java.util.List;
import java.util.Locale;

public final class OcrLog {

    private static final String TAG = "LensLingoDebug";
    private static int run = 0;

    private OcrLog() {}

    public static synchronized void startRun(int width, int height) {
        run++;
        d("===== RUN " + run + " image=" + width + "x" + height + " =====");
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void raw(String label, String text) {
        for (int i = 0; i < text.length(); i += 3000) {
            Log.d(TAG, label + "[" + (i / 3000) + "] "
                    + text.substring(i, Math.min(text.length(), i + 3000)));
        }
    }

    public static void dumpWords(String stage, List<OcrWord> words) {
        d("--- " + stage + " (" + words.size() + " words) ---");
        for (int i = 0; i < words.size(); i++) {
            OcrWord w = words.get(i);
            d(String.format(Locale.US,
                    "#%d L%d \"%s\" shown=\"%s\" tr=\"%s\" kind=%s hl=%b conf=%.2f box=%s line=%s",
                    i + 1, w.lineIndex, w.text, w.shownText(),
                    w.translation == null ? "" : w.translation,
                    kindName(w.kind), w.highlight, w.confidence,
                    fmt(w.box()), fmt(w.lineBox)));
        }
    }

    public static String fmt(float[] v) {
        if (v == null) return "-";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(Locale.US, "%.3f", v[i]));
        }
        return sb.append("]").toString();
    }

    private static String kindName(int kind) {
        switch (kind) {
            case OcrWord.KIND_NAME: return "name";
            case OcrWord.KIND_NOISE: return "noise";
            default: return "word";
        }
    }
}
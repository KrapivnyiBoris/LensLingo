package com.example.lenslingo.ocr;

import android.graphics.Point;
import android.graphics.Rect;

import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.data.model.OcrWord;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalMLDataSource {
    private static final float CONTEXT_MIN_CONFIDENCE = 0.6f;

    private static final String SPLIT_REGEX =
            "(?<=[,;:!?])(?=\\p{L})|(?<=\\p{Ll}\\.)(?=\\p{Lu})";
    private static final float MIN_CONFIDENCE = 0.40f;
    private static final String WORD_PATTERN =
            "^[\\p{Ps}\\p{Pi}\"']*\\p{L}+(?:['’\\-]\\p{L}+)*\\p{P}*$";

    private final TextRecognizer recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public interface OcrCallback {
        void onSuccess(OcrResult result);
        void onFailure(Exception e);
    }

    public void recognizeText(InputImage image, OcrCallback callback) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<OcrWord> words = new ArrayList<>();
                    List<OcrWord> contextWords = new ArrayList<>();
                    int lineIndex = 0;

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            lineIndex++;
                            float[] lineBox = toBox(line.getBoundingBox(), width, height);

                            for (Text.Element element : line.getElements()) {
                                float[] corners = toCorners(element, width, height);
                                if (corners == null) continue;

                                String raw = element.getText().trim();
                                String[] parts = raw.split(SPLIT_REGEX);
                                int total = Math.max(1, raw.length());
                                int offset = 0;

                                for (String part : parts) {
                                    float t0 = offset / (float) total;
                                    offset += part.length();
                                    float t1 = offset / (float) total;

                                    String text = fixCase(part.trim());
                                    float[] c = parts.length == 1 ? corners : sliceCorners(corners, t0, t1);
                                    String reason = rejectReason(text, element.getConfidence());

                                    if (reason == null) {
                                        OcrWord word = new OcrWord(text, null, c, element.getConfidence(), lineIndex);
                                        word.lineBox = lineBox;
                                        words.add(word);
                                        contextWords.add(word);
                                        continue;
                                    }

                                    boolean keepForContext = "short".equals(reason)
                                            && element.getConfidence() >= CONTEXT_MIN_CONFIDENCE
                                            && text.matches(WORD_PATTERN);
                                    if (keepForContext) {
                                        contextWords.add(new OcrWord(text, null, c, element.getConfidence(), lineIndex));
                                    }
                                    OcrLog.d(String.format(Locale.US, "%s \"%s\" reason=%s conf=%.2f",
                                            keepForContext ? "CONTEXT" : "DROP", text, reason, element.getConfidence()));
                                }
                            }
                        }
                    }

                    OcrResult parsed = new OcrResult(words, "auto", false);
                    parsed.contextWords = contextWords;
                    callback.onSuccess(parsed);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Вирізає частину рамки між частками t0..t1 уздовж слова
    private float[] sliceCorners(float[] c, float t0, float t1) {
        return new float[]{
                lerp(c[0], c[2], t0), lerp(c[1], c[3], t0),
                lerp(c[0], c[2], t1), lerp(c[1], c[3], t1),
                lerp(c[6], c[4], t1), lerp(c[7], c[5], t1),
                lerp(c[6], c[4], t0), lerp(c[7], c[5], t0)};
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private String rejectReason(String text, float confidence) {
        if (text.length() < 2) return "short";
        if (confidence < MIN_CONFIDENCE) return "low_conf";
        if (!text.matches(WORD_PATTERN)) return "charset";
        return null;
    }

    private float[] toBox(Rect r, int width, int height) {
        if (r == null) return null;
        return new float[]{r.left / (float) width, r.top / (float) height,
                r.right / (float) width, r.bottom / (float) height};
    }

    private float[] toCorners(Text.Element element, int width, int height) {
        Point[] points = element.getCornerPoints();
        if (points != null && points.length == 4) {
            float[] out = new float[8];
            for (int i = 0; i < 4; i++) {
                out[i * 2] = points[i].x / (float) width;
                out[i * 2 + 1] = points[i].y / (float) height;
            }
            return out;
        }

        float[] b = toBox(element.getBoundingBox(), width, height);
        if (b == null) return null;
        return new float[]{b[0], b[1], b[2], b[1], b[2], b[3], b[0], b[3]};
    }

    private String fixCase(String t) {
        int n = t.length();
        if (n < 3) return t;
        char last = t.charAt(n - 1);
        if (!Character.isUpperCase(last)) return t;
        for (int i = 0; i < n - 1; i++) {
            if (!Character.isLowerCase(t.charAt(i))) return t;
        }
        return t.substring(0, n - 1) + Character.toLowerCase(last);
    }
}
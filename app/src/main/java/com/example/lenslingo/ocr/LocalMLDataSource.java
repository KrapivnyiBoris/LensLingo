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

public class LocalMLDataSource {

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
                    int lineIndex = 0;

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            lineIndex++;
                            float[] lineBox = toBox(line.getBoundingBox(), width, height);

                            for (Text.Element element : line.getElements()) {
                                String text = element.getText().trim();
                                String reason = rejectReason(text, element.getConfidence());
                                if (reason != null) {
                                    OcrLog.d(String.format(java.util.Locale.US, "DROP \"%s\" reason=%s conf=%.2f",
                                            text, reason, element.getConfidence()));
                                    continue;
                                }

                                float[] corners = toCorners(element, width, height);
                                if (corners == null) continue;

                                OcrWord word = new OcrWord(text, null, corners,
                                        element.getConfidence(), lineIndex);
                                word.lineBox = lineBox;
                                words.add(word);
                            }
                        }
                    }

                    callback.onSuccess(new OcrResult(words, "auto", false));
                })
                .addOnFailureListener(callback::onFailure);
    }

    private static final float MIN_CONFIDENCE = 0.40f;
    private static final String WORD_PATTERN =
            "^[\\p{Ps}\\p{Pi}\"']*\\p{L}+(?:['’\\-]\\p{L}+)*\\p{P}*$";

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
}
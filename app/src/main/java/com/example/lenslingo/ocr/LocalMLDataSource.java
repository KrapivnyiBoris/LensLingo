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

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            for (Text.Element element : line.getElements()) {
                                String text = element.getText().trim();
                                if (!isMeaningful(text)) continue;

                                float[] corners = toCorners(element, width, height);
                                if (corners == null) continue;

                                words.add(new OcrWord(text, null, corners));
                            }
                        }
                    }

                    callback.onSuccess(new OcrResult(words, "auto", false));
                })
                .addOnFailureListener(callback::onFailure);
    }

    private boolean isMeaningful(String text) {
        if (text.length() < 2) return false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) return true;
        }
        return false;
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

        Rect box = element.getBoundingBox();
        if (box == null) return null;
        float l = box.left / (float) width;
        float t = box.top / (float) height;
        float r = box.right / (float) width;
        float b = box.bottom / (float) height;
        return new float[]{l, t, r, t, r, b, l, b};
    }
}
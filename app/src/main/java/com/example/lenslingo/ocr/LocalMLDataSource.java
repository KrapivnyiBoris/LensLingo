package com.example.lenslingo.ocr;

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
                                Rect box = element.getBoundingBox();
                                int[] bbox = box != null ? normalize(box, width, height) : null;
                                words.add(new OcrWord(element.getText(), null, bbox));
                            }
                        }
                    }

                    callback.onSuccess(new OcrResult(words, "auto", false));
                })
                .addOnFailureListener(callback::onFailure);
    }

    private int[] normalize(Rect box, int width, int height) {
        return new int[]{
                box.top * 1000 / height,
                box.left * 1000 / width,
                box.bottom * 1000 / height,
                box.right * 1000 / width
        };
    }
}
package com.example.lenslingo.ocr;

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
        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<OcrWord> words = new ArrayList<>();

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            for (Text.Element element : line.getElements()) {
                                android.graphics.Rect box = element.getBoundingBox();
                                int[] bbox = box != null
                                        ? new int[]{box.top, box.left, box.bottom, box.right}
                                        : null;
                                words.add(new OcrWord(element.getText(), null, bbox));
                            }
                        }
                    }

                    callback.onSuccess(new OcrResult(words, "auto", false));
                })
                .addOnFailureListener(callback::onFailure);
    }
}
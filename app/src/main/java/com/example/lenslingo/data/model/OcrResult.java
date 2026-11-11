package com.example.lenslingo.data.model;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OcrResult {
    public final List<OcrWord> words;
    public final String sourceLanguage;
    public final boolean fromOnlineMode;

    public Bitmap image;
    public String imageUri;
    public String notice;
    public Map<Integer, String> lineTranslations;

    public OcrResult(List<OcrWord> words, String sourceLanguage, boolean fromOnlineMode) {
        this.words = words;
        this.sourceLanguage = sourceLanguage;
        this.fromOnlineMode = fromOnlineMode;
    }

    public List<OcrWord> visibleWords() {
        List<OcrWord> visible = new ArrayList<>();
        for (OcrWord word : words) {
            if (word.highlight) visible.add(word);
        }
        return visible;
    }
}
package com.example.lenslingo.data.model;

import android.graphics.Bitmap;

import java.util.List;

public class OcrResult {
    public final List<OcrWord> words;
    public final String sourceLanguage;
    public final boolean fromOnlineMode;

    public Bitmap image;
    public String imageUri;

    public OcrResult(List<OcrWord> words, String sourceLanguage, boolean fromOnlineMode) {
        this.words = words;
        this.sourceLanguage = sourceLanguage;
        this.fromOnlineMode = fromOnlineMode;
    }
}
package com.example.lenslingo.data.model;

import java.util.List;

public class OcrResult {
    public final List<OcrWord> words;
    public final String sourceLanguage;
    public final boolean fromOnlineMode;

    public OcrResult(List<OcrWord> words, String sourceLanguage, boolean fromOnlineMode) {
        this.words = words;
        this.sourceLanguage = sourceLanguage;
        this.fromOnlineMode = fromOnlineMode;
    }
}
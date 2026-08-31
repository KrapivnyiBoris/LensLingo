package com.example.lenslingo.ocr;

import java.util.List;

class GeminiOcrPayload {
    String sourceLanguage;
    List<WordDto> words;

    static class WordDto {
        String text;
        String translation;
        int[] box;
    }
}
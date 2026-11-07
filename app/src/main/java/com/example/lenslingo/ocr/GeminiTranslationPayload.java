package com.example.lenslingo.ocr;

import java.util.List;

class GeminiTranslationPayload {
    String sourceLanguage;
    List<Item> translations;

    static class Item {
        int id;
        String t;
    }
}
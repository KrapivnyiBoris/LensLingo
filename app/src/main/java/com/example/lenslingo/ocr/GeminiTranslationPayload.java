package com.example.lenslingo.ocr;

import java.util.List;

public class GeminiTranslationPayload {
    public String sourceLanguage;
    public List<Item> items;

    public static class Item {
        public int id;
        public String w;
        public String t;
        public String k;
        public String c;
    }

    public List<Line> lines;

    public static class Line {
        public int n;
        public String t;
    }
}
package com.example.lenslingo.data.model;

public class OcrWord {
    public final String text;
    public final String translation;
    public final int[] boundingBox;

    public OcrWord(String text, String translation, int[] boundingBox) {
        this.text = text;
        this.translation = translation;
        this.boundingBox = boundingBox;
    }
}
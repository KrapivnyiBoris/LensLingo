package com.example.lenslingo.data.model;

public class OcrWord {
    public final String text;
    public String translation;
    public final float[] corners;
    public boolean highlight = true;

    public OcrWord(String text, String translation, float[] corners) {
        this.text = text;
        this.translation = translation;
        this.corners = corners;
    }
}
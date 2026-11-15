package com.example.lenslingo.data.model;

public class OcrWord {
    public static final int KIND_WORD = 0;
    public static final int KIND_NAME = 1;
    public static final int KIND_NOISE = 2;

    public final String text;
    public final float[] corners;
    public final float confidence;
    public final int lineIndex;
    public float[] lineBox;

    public String displayText;
    public String translation;
    public int kind = KIND_WORD;
    public boolean highlight = true;

    public String altTranslation;
    public boolean fromGlossary;

    public OcrWord(String text, String translation, float[] corners,
                   float confidence, int lineIndex) {
        this.text = text;
        this.translation = translation;
        this.corners = corners;
        this.confidence = confidence;
        this.lineIndex = lineIndex;
    }

    public String shownText() {
        return displayText != null && !displayText.isEmpty() ? displayText : text;
    }

    public float[] box() {
        if (corners == null || corners.length != 8) return null;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, corners[i * 2]);
            maxX = Math.max(maxX, corners[i * 2]);
            minY = Math.min(minY, corners[i * 2 + 1]);
            maxY = Math.max(maxY, corners[i * 2 + 1]);
        }
        return new float[]{minX, minY, maxX, maxY};
    }
}
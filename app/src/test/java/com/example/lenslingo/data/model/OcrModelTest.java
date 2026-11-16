package com.example.lenslingo.data.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class OcrModelTest {

    private static OcrWord word(String text, float[] corners) {
        return new OcrWord(text, null, corners, 0.9f, 1);
    }

    // T11: габаритна рамка нахиленого слова (чотири кути) дає мінімум і максимум
    @Test
    public void box_ofSlantedWord_coversAllCorners() {
        float[] corners = {0.2f, 0.1f, 0.6f, 0.2f, 0.6f, 0.4f, 0.2f, 0.3f};
        float[] box = word("tilted", corners).box();
        assertArrayEquals(new float[]{0.2f, 0.1f, 0.6f, 0.4f}, box, 0.0001f);
    }

    // T12: слово без кутів не має рамки
    @Test
    public void box_withoutCorners_isNull() {
        assertNull(word("x", null).box());
    }

    // T13: показується виправлене слово, а якщо його нема, то прочитане
    @Test
    public void shownText_prefersCorrectedWord() {
        OcrWord w = word("eptember", new float[8]);
        assertEquals("eptember", w.shownText());

        w.displayText = "";
        assertEquals("eptember", w.shownText());

        w.displayText = "September";
        assertEquals("September", w.shownText());
    }

    // T14: у результат для екрана потрапляють лише підсвічені слова
    @Test
    public void visibleWords_excludesHiddenOnes() {
        OcrWord shown = word("hello", new float[8]);
        OcrWord hidden = word("ZYN", new float[8]);
        hidden.highlight = false;

        List<OcrWord> all = new ArrayList<>();
        all.add(shown);
        all.add(hidden);

        List<OcrWord> visible = new OcrResult(all, "en", true).visibleWords();
        assertEquals(1, visible.size());
        assertEquals("hello", visible.get(0).text);
    }
}
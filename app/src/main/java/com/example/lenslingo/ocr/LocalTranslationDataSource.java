package com.example.lenslingo.ocr;

import com.example.lenslingo.data.model.OcrWord;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LocalTranslationDataSource {

    public static final Set<String> SUPPORTED =
            new HashSet<>(Arrays.asList("en", "es", "de", "fr", "it"));

    public static class Outcome {
        public String language = "auto";
        public String notice;
    }

    private final LanguageIdentifier identifier = LanguageIdentification.getClient();

    // Блокуючий метод: викликати лише з фонового потоку
    public Outcome translateWords(List<OcrWord> words) {
        Outcome out = new Outcome();

        StringBuilder text = new StringBuilder();
        for (OcrWord w : words) text.append(w.text).append(' ');

        String lang;
        try {
            lang = Tasks.await(identifier.identifyLanguage(text.toString()));
        } catch (Exception e) {
            OcrLog.d("LangID failed: " + e);
            out.notice = "Could not detect the language";
            return out;
        }

        if ("und".equals(lang)) {
            out.notice = "Could not detect the language";
            return out;
        }
        out.language = lang;
        OcrLog.d("LangID: " + lang);

        String source = TranslateLanguage.fromLanguageTag(lang);
        if (!SUPPORTED.contains(lang) || source == null) {
            out.notice = "Offline translation is not available for \"" + lang + "\"";
            return out;
        }

        Translator translator = Translation.getClient(new TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(TranslateLanguage.UKRAINIAN)
                .build());

        try {
            long t0 = System.currentTimeMillis();
            Tasks.await(translator.downloadModelIfNeeded(new DownloadConditions.Builder().build()));
            OcrLog.d("Translate model ready in " + (System.currentTimeMillis() - t0) + " ms");
        } catch (Exception e) {
            OcrLog.d("Translate model unavailable: " + e);
            out.notice = "Offline translation needs a one-time download. Connect to the internet and try again.";
            translator.close();
            return out;
        }

        Map<String, String> cache = new HashMap<>();
        for (OcrWord w : words) {
            String clean = w.text.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
            String key = clean.toLowerCase(Locale.ROOT);

            String translated = cache.get(key);
            if (translated == null) {
                try {
                    translated = Tasks.await(translator.translate(clean)).trim();
                } catch (Exception e) {
                    translated = "";
                }
                cache.put(key, translated);
            }

            boolean useful = !translated.isEmpty() && !translated.equalsIgnoreCase(clean);
            w.translation = useful ? translated : null;
            w.highlight = useful;
            if (!clean.isEmpty() && !clean.equals(w.text)) w.displayText = clean;
        }

        translator.close();
        return out;
    }

    // Неблокуючий: фонове завантаження моделі після успішного онлайн-запиту
    public void prefetch(String lang) {
        String source = TranslateLanguage.fromLanguageTag(lang == null ? "" : lang);
        if (source == null || !SUPPORTED.contains(lang)) return;

        Translator translator = Translation.getClient(new TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(TranslateLanguage.UKRAINIAN)
                .build());
        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnCompleteListener(task -> {
                    OcrLog.d("Prefetch " + lang + ": " + (task.isSuccessful() ? "ok" : "failed"));
                    translator.close();
                });
    }
}
package com.example.lenslingo.ocr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.lenslingo.data.model.OcrWord;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
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

    private final Context appContext;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LanguageIdentifier identifier = LanguageIdentification.getClient();

    public LocalTranslationDataSource(Context context) {
        this.appContext = context.getApplicationContext();
    }

    private void toast(String message) {
        main.post(() -> Toast.makeText(appContext, message, Toast.LENGTH_LONG).show());
    }

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

        boolean downloaded = false;
        try {
            downloaded = Tasks.await(RemoteModelManager.getInstance()
                    .isModelDownloaded(new TranslateRemoteModel.Builder(source).build()));
        } catch (Exception ignored) {
        }
        if (!downloaded) {
            toast("Downloading the language pack (" + lang + "), one time only…");
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
            if (!clean.isEmpty() && !clean.equals(w.text)) w.displayText = clean;

            if (isArticle(lang, key)) {
                w.translation = "(артикль)";
                w.highlight = true;
                continue;
            }

            String translated = cache.get(key);
            if (translated == null) {
                try {
                    translated = tidy(Tasks.await(translator.translate(clean)), clean);
                } catch (Exception e) {
                    translated = "";
                }
                cache.put(key, translated);
            }

            boolean useful = !translated.isEmpty() && !translated.equalsIgnoreCase(clean);
            w.translation = useful ? translated : null;
            w.highlight = useful;
        }

        translator.close();
        return out;
    }

    private String tidy(String raw, String source) {
        String t = raw.trim().replaceAll("[\\p{P}&&[^()]]+$", "").trim();
        if (t.isEmpty()) return "";
        if (t.matches("[\\p{N}\\p{P}\\s]*") && !source.matches(".*\\p{N}.*")) return "";
        if (!source.isEmpty() && Character.isLowerCase(source.charAt(0))) {
            t = Character.toLowerCase(t.charAt(0)) + t.substring(1);
        }
        return t;
    }

    private static boolean isArticle(String lang, String w) {
        switch (lang) {
            case "en": return Arrays.asList("the", "a", "an").contains(w);
            case "es": return Arrays.asList("el", "la", "los", "las", "un", "una", "unos", "unas", "lo").contains(w);
            case "de": return Arrays.asList("der", "die", "das", "den", "dem", "des",
                    "ein", "eine", "einen", "einem", "einer").contains(w);
            case "fr": return Arrays.asList("le", "la", "les", "un", "une", "des", "du").contains(w);
            case "it": return Arrays.asList("il", "lo", "la", "i", "gli", "le", "un", "una", "uno").contains(w);
            default: return false;
        }
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
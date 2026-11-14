package com.example.lenslingo.ocr;

import com.example.lenslingo.BuildConfig;

import java.util.Locale;

public class TranslationSuggester {

    private static final long TIMEOUT_MS = 5000;

    private final RemoteDataSource remote = new RemoteDataSource();
    private final String[] models = BuildConfig.GEMINI_MODELS.split("\\s*,\\s*");

    public void suggest(String word, String lang, RemoteDataSource.SuggestionCallback callback) {
        String name = (lang == null || "auto".equals(lang))
                ? "foreign" : new Locale(lang).getDisplayLanguage(Locale.ENGLISH);
        attempt(0, word, name, callback);
    }

    private void attempt(int index, String word, String name,
                         RemoteDataSource.SuggestionCallback callback) {
        remote.suggest(models[index], TIMEOUT_MS, word, name,
                new RemoteDataSource.SuggestionCallback() {
                    @Override
                    public void onSuccess(java.util.List<String> variants) {
                        callback.onSuccess(variants);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (index + 1 < models.length) attempt(index + 1, word, name, callback);
                        else callback.onFailure(e);
                    }
                });
    }
}
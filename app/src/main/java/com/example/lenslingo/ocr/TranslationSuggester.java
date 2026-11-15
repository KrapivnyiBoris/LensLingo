package com.example.lenslingo.ocr;

import com.example.lenslingo.BuildConfig;

import java.util.Locale;

public class TranslationSuggester {

    private static final long TIMEOUT_MS = 5000;

    private final RemoteDataSource remote = new RemoteDataSource();
    private final String[] models = BuildConfig.GEMINI_MODELS.split("\\s*,\\s*");

    private static final android.util.LruCache<String, java.util.List<String>> CACHE =
            new android.util.LruCache<>(100);

    public void suggest(String word, String lang, RemoteDataSource.SuggestionCallback callback) {
        final String key = (lang == null ? "auto" : lang) + "|" + word.trim().toLowerCase(Locale.ROOT);

        java.util.List<String> cached = CACHE.get(key);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        String name = (lang == null || "auto".equals(lang))
                ? "foreign" : new Locale(lang).getDisplayLanguage(Locale.ENGLISH);

        attempt(0, word, name, new RemoteDataSource.SuggestionCallback() {
            @Override
            public void onSuccess(java.util.List<String> variants) {
                if (!variants.isEmpty()) CACHE.put(key, variants);
                callback.onSuccess(variants);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
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
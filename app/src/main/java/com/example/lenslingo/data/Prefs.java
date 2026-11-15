package com.example.lenslingo.data;

import android.content.Context;

public final class Prefs {

    private static final String FILE = "lenslingo_prefs";
    private static final String ONLINE_TRANSLATION = "online_translation";
    private static final String ONLINE_SUGGESTIONS = "online_suggestions";

    private Prefs() {}

    public static boolean onlineTranslation(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(ONLINE_TRANSLATION, true);
    }

    public static void setOnlineTranslation(Context c, boolean value) {
        c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(ONLINE_TRANSLATION, value).apply();
    }

    public static boolean onlineSuggestions(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(ONLINE_SUGGESTIONS, true);
    }

    public static void setOnlineSuggestions(Context c, boolean value) {
        c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(ONLINE_SUGGESTIONS, value).apply();
    }
}
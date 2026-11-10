package com.example.lenslingo.data.local;

import android.content.Context;

public final class AppDatabaseHolder {
    private AppDatabaseHolder() {}

    public static int countByPhoto(Context context, String uri) {
        return AppDatabase.getInstance(context).wordDao().countByPhotoSync(uri);
    }
}
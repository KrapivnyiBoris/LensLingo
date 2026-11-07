package com.example.lenslingo.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.data.model.OcrWord;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OCRRepository {

    private final Context appContext;
    private final LocalMLDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public OCRRepository(Context context) {
        appContext = context.getApplicationContext();
        localDataSource = new LocalMLDataSource();
        remoteDataSource = new RemoteDataSource();
    }

    public interface OcrCallback {
        void onSuccess(OcrResult result, boolean wasOffline);
        void onFailure(Exception e);
    }

    public void processImage(Bitmap bitmap, OcrCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        localDataSource.recognizeText(image, new LocalMLDataSource.OcrCallback() {
            @Override
            public void onSuccess(OcrResult local) {
                if (local.words.isEmpty()) {
                    callback.onFailure(new IllegalStateException("No text found"));
                } else if (isNetworkAvailable()) {
                    translateOnline(bitmap, local, callback);
                } else {
                    callback.onSuccess(local, true);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private void translateOnline(Bitmap bitmap, OcrResult local, OcrCallback callback) {
        executor.execute(() -> {
            String base64 = ImageUtils.toBase64(bitmap);

            remoteDataSource.translateWords(base64, local.words, new RemoteDataSource.TranslationCallback() {
                @Override
                public void onSuccess(String sourceLanguage, Map<Integer, String> translations) {
                    List<OcrWord> words = local.words;
                    for (int i = 0; i < words.size(); i++) {
                        OcrWord word = words.get(i);
                        String t = translations.get(i + 1);
                        boolean useful = t != null && !t.trim().isEmpty()
                                && !t.trim().equalsIgnoreCase(word.text);
                        word.translation = useful ? t.trim() : null;
                        word.highlight = useful;
                    }
                    callback.onSuccess(new OcrResult(words, sourceLanguage, true), false);
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("LensLingo", "Gemini failed, using offline result", e);
                    callback.onSuccess(local, true);
                }
            });
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
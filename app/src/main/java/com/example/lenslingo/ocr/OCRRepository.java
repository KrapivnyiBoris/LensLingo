package com.example.lenslingo.ocr;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;

import com.example.lenslingo.data.model.OcrResult;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;

public class OCRRepository {

    private final Context appContext;
    private final LocalMLDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;

    public OCRRepository(Context context) {
        appContext = context.getApplicationContext();
        localDataSource = new LocalMLDataSource();
        remoteDataSource = new RemoteDataSource();
    }

    public interface OcrCallback {
        void onSuccess(OcrResult result, boolean wasOffline);
        void onFailure(Exception e);
    }

    public void processImage(Uri imageUri, String base64Image, OcrCallback callback) {
        if (isNetworkAvailable()) {
            remoteDataSource.recognizeAndTranslate(base64Image, new RemoteDataSource.OcrCallback() {
                @Override
                public void onSuccess(OcrResult result) {
                    callback.onSuccess(result, false);
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("LensLingo", "Gemini failed, falling back to offline", e);
                    fallbackToOffline(imageUri, callback);
                }
            });
        } else {
            fallbackToOffline(imageUri, callback);
        }
    }

    private void fallbackToOffline(Uri imageUri, OcrCallback callback) {
        try {
            InputImage image = InputImage.fromFilePath(appContext, imageUri);
            localDataSource.recognizeText(image, new LocalMLDataSource.OcrCallback() {
                @Override
                public void onSuccess(OcrResult result) {
                    callback.onSuccess(result, true);
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });
        } catch (IOException e) {
            callback.onFailure(e);
        }
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
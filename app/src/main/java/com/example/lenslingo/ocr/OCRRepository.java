package com.example.lenslingo.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.example.lenslingo.data.model.OcrResult;
import com.google.mlkit.vision.common.InputImage;

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

    public void processImage(Bitmap bitmap, OcrCallback callback) {
        if (isNetworkAvailable()) {
            String base64 = ImageUtils.toBase64(bitmap);
            remoteDataSource.recognizeAndTranslate(base64, new RemoteDataSource.OcrCallback() {
                @Override
                public void onSuccess(OcrResult result) {
                    callback.onSuccess(result, false);
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("LensLingo", "Gemini failed, falling back to offline", e);
                    fallbackToOffline(bitmap, callback);
                }
            });
        } else {
            fallbackToOffline(bitmap, callback);
        }
    }

    private void fallbackToOffline(Bitmap bitmap, OcrCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
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
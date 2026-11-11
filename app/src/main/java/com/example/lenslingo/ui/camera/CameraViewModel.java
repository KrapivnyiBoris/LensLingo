package com.example.lenslingo.ui.camera;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lenslingo.R;
import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.ocr.ImageUtils;
import com.example.lenslingo.ocr.OCRRepository;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraViewModel extends AndroidViewModel {

    private final OCRRepository ocrRepository;
    private final ExecutorService executor;

    private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);
    private final MutableLiveData<OcrResult> ocrResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public CameraViewModel(@NonNull Application application) {
        super(application);
        ocrRepository = new OCRRepository(application);
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<Boolean> getIsProcessing() {
        return isProcessing;
    }

    public LiveData<OcrResult> getOcrResult() {
        return ocrResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void consumeResult() {
        ocrResult.setValue(null);
    }

    public void processImage(Uri imageUri) {
        isProcessing.setValue(true);

        executor.execute(() -> {
            try {
                Bitmap bitmap = ImageUtils.loadUprightBitmap(getApplication(), imageUri);

                ocrRepository.processImage(bitmap, new OCRRepository.OcrCallback() {
                    @Override
                    public void onSuccess(OcrResult result, boolean wasOffline) {
                        result.image = bitmap;
                        result.imageUri = imageUri.toString();
                        isProcessing.postValue(false);
                        ocrResult.postValue(result);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        isProcessing.postValue(false);
                        errorMessage.postValue(getApplication().getString(R.string.msg_text_not_found));
                    }
                });

            } catch (IOException e) {
                isProcessing.postValue(false);
                errorMessage.postValue(getApplication().getString(R.string.msg_read_error));
            }
        });
    }
}
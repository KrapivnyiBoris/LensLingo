package com.example.lenslingo.ui.camera;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.ocr.ImageUtils;
import com.example.lenslingo.ocr.OCRRepository;
import com.google.mlkit.vision.common.InputImage;

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

    public void processImage(Uri imageUri) {
        isProcessing.setValue(true);

        executor.execute(() -> {
            try {
                String base64 = ImageUtils.uriToBase64(getApplication(), imageUri);

                ocrRepository.processImage(imageUri, base64, new OCRRepository.OcrCallback() {
                    @Override
                    public void onSuccess(OcrResult result, boolean wasOffline) {
                        isProcessing.postValue(false);
                        ocrResult.postValue(result);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        isProcessing.postValue(false);
                        errorMessage.postValue("Text not found. Try another photo.");
                    }
                });

            } catch (IOException e) {
                isProcessing.postValue(false);
                errorMessage.postValue("Could not read image.");
            }
        });
    }
}
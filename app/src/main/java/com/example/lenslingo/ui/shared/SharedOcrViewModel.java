package com.example.lenslingo.ui.shared;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lenslingo.data.model.OcrResult;

public class SharedOcrViewModel extends ViewModel {

    private final MutableLiveData<OcrResult> ocrResult = new MutableLiveData<>();

    public void setResult(OcrResult result) {
        ocrResult.setValue(result);
    }

    public LiveData<OcrResult> getResult() {
        return ocrResult;
    }
}
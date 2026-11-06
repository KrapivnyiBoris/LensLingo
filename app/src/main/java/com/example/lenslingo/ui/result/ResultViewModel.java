package com.example.lenslingo.ui.result;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lenslingo.data.local.WordEntity;
import com.example.lenslingo.data.model.OcrWord;
import com.example.lenslingo.repository.WordRepository;

public class ResultViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final MutableLiveData<String> saveMessage = new MutableLiveData<>();

    public ResultViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application);
    }

    public LiveData<String> getSaveMessage() {
        return saveMessage;
    }

    public void saveWord(OcrWord ocrWord, String sourceLanguage, String photoUri) {
        if (ocrWord.translation == null || ocrWord.translation.isEmpty()) {
            saveMessage.postValue("No translation available for this word");
            return;
        }

        WordEntity entity = new WordEntity();
        entity.word = ocrWord.text;
        entity.translation = ocrWord.translation;
        entity.sourceLanguage = sourceLanguage != null ? sourceLanguage : "auto";
        entity.photoUri = photoUri;
        entity.dateAdded = System.currentTimeMillis();
        entity.nextReviewDate = System.currentTimeMillis();

        repository.insert(entity, saved -> {
            String message = saved ? "Word saved" : "Word already in dictionary";
            saveMessage.postValue(message);
        });
    }
}
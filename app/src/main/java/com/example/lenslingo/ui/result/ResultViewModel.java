package com.example.lenslingo.ui.result;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.AppDatabaseHolder;
import com.example.lenslingo.data.local.PhotoStore;
import com.example.lenslingo.data.local.WordEntity;
import com.example.lenslingo.data.model.OcrWord;
import com.example.lenslingo.repository.WordRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<String> saveMessage = new MutableLiveData<>();

    // Одна постійна копія фото на весь скан
    private Bitmap cachedSource;
    private String storedPhotoUri;

    public ResultViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application);
    }

    public LiveData<String> getSaveMessage() {
        return saveMessage;
    }

    public void saveWord(OcrWord ocrWord, String sourceLanguage, Bitmap image) {
        if (ocrWord.translation == null || ocrWord.translation.isEmpty()) {
            saveMessage.postValue(getApplication().getString(R.string.msg_no_translation));
            return;
        }
        if (ocrWord.translation.startsWith("(")) {
            saveMessage.postValue(getApplication().getString(R.string.msg_no_standalone));
            return;
        }

        executor.execute(() -> {
            if (image != null && image != cachedSource) {
                cachedSource = image;
                storedPhotoUri = PhotoStore.save(getApplication(), image);
            }

            WordEntity entity = new WordEntity();
            entity.word = ocrWord.shownText();
            entity.translation = ocrWord.translation;
            entity.sourceLanguage = sourceLanguage != null ? sourceLanguage : "auto";
            entity.photoUri = storedPhotoUri;
            entity.dateAdded = System.currentTimeMillis();
            entity.nextReviewDate = System.currentTimeMillis();

            repository.insert(entity, saved -> {
                if (!saved && storedPhotoUri != null) {
                    cleanupIfUnused();
                }
                saveMessage.postValue(getApplication().getString(
                        saved ? R.string.msg_word_saved : R.string.msg_word_exists));
            });
        });
    }

    private void cleanupIfUnused() {
        String uri = storedPhotoUri;
        executor.execute(() -> {
            // Якщо це був перший дублікат і фото ще нікому не потрібне, файл прибираємо
            if (uri != null && AppDatabaseHolder.countByPhoto(getApplication(), uri) == 0) {
                PhotoStore.delete(uri);
                if (uri.equals(storedPhotoUri)) {
                    storedPhotoUri = null;
                    cachedSource = null;
                }
            }
        });
    }
}
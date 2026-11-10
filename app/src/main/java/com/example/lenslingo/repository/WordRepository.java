package com.example.lenslingo.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.lenslingo.data.local.AppDatabase;
import com.example.lenslingo.data.local.PhotoStore;
import com.example.lenslingo.data.local.WordDao;
import com.example.lenslingo.data.local.WordEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WordRepository {

    private final WordDao wordDao;
    private final ExecutorService executor;

    private final LiveData<List<WordEntity>> allWords;

    public WordRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        wordDao = db.wordDao();
        executor = Executors.newFixedThreadPool(4);
        allWords = wordDao.getAllWords();
    }

    public LiveData<List<WordEntity>> getAllWords() {
        return allWords;
    }

    public LiveData<List<WordEntity>> searchWords(String query) {
        return wordDao.searchWords(query);
    }

    public interface SaveCallback {
        void onResult(boolean saved);
    }

    public void insert(WordEntity word, SaveCallback callback) {
        executor.execute(() -> {
            long resultId = wordDao.insert(word);
            boolean saved = resultId != -1;
            if (callback != null) {
                callback.onResult(saved);
            }
        });
    }

    public void update(WordEntity word) {
        executor.execute(() -> wordDao.update(word));
    }

    public void delete(WordEntity word) {
        executor.execute(() -> {
            wordDao.delete(word);
            if (word.photoUri != null && wordDao.countByPhotoSync(word.photoUri) == 0) {
                PhotoStore.delete(word.photoUri);
            }
        });
    }

    public List<WordEntity> getWordsDueForReviewSync() {
        Future<List<WordEntity>> future = executor.submit(() ->
                wordDao.getWordsDueForReview(System.currentTimeMillis())
        );
        try {
            return future.get();
        } catch (Exception e) {
            return List.of();
        }
    }
}

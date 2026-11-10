package com.example.lenslingo.ui.dictionary;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.lenslingo.data.local.WordEntity;
import com.example.lenslingo.repository.WordRepository;

import java.util.List;

public class DictionaryViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final LiveData<List<WordEntity>> words;

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public DictionaryViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application);

        words = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return repository.getAllWords();
            } else {
                return repository.searchWords(query);
            }
        });
    }

    public LiveData<List<WordEntity>> getWords() {
        return words;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void deleteWord(WordEntity word) {
        repository.delete(word);
    }

    public void deleteWordById(int id) {
        repository.deleteById(id);
    }
}

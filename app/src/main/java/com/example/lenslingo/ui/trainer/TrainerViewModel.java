package com.example.lenslingo.ui.trainer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lenslingo.data.local.WordEntity;
import com.example.lenslingo.repository.WordRepository;
import com.example.lenslingo.sm2.SM2Calculator;
import com.example.lenslingo.sm2.SM2Result;
import com.example.lenslingo.sm2.SM2Scheduler;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrainerViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final SM2Scheduler scheduler;
    private final SM2Calculator calculator;
    private final ExecutorService executor;

    private final LinkedList<WordEntity> sessionQueue = new LinkedList<>();

    private final MutableLiveData<WordEntity> currentCard = new MutableLiveData<>();
    private final MutableLiveData<Integer> remainingCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> sessionFinished = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> noWordsToday = new MutableLiveData<>(false);

    public TrainerViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application);
        scheduler = new SM2Scheduler(repository);
        calculator = new SM2Calculator();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<WordEntity> getCurrentCard() {
        return currentCard;
    }

    public LiveData<Integer> getRemainingCount() {
        return remainingCount;
    }

    public LiveData<Boolean> getSessionFinished() {
        return sessionFinished;
    }

    public LiveData<Boolean> getNoWordsToday() {
        return noWordsToday;
    }

    public void startSession() {
        executor.execute(() -> {
            List<WordEntity> dueWords = scheduler.getWordsDueToday();

            if (dueWords.isEmpty()) {
                noWordsToday.postValue(true);
                return;
            }

            sessionQueue.clear();
            sessionQueue.addAll(dueWords);

            showNextCard();
        });
    }

    public void submitAnswer(int quality) {
        WordEntity word = currentCard.getValue();
        if (word == null) return;

        SM2Result result = calculator.calculate(
                quality,
                word.easeFactor,
                word.repetitionCount,
                word.intervalDays
        );

        word.easeFactor = result.easeFactor;
        word.repetitionCount = result.repetitionCount;
        word.intervalDays = result.intervalDays;
        word.nextReviewDate = result.nextReviewDate;

        if (quality < 3) {
            sessionQueue.addLast(word);
        } else {
            repository.update(word);
        }

        sessionQueue.pollFirst();
        showNextCard();
    }

    private void showNextCard() {
        if (sessionQueue.isEmpty()) {
            currentCard.postValue(null);
            sessionFinished.postValue(true);
        } else {
            currentCard.postValue(sessionQueue.peekFirst());
            remainingCount.postValue(sessionQueue.size());
        }
    }
}
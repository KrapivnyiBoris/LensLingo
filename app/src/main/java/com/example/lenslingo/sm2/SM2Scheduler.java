package com.example.lenslingo.sm2;

import com.example.lenslingo.data.local.WordEntity;
import com.example.lenslingo.repository.WordRepository;

import java.util.List;

public class SM2Scheduler {

    private final WordRepository repository;

    public SM2Scheduler(WordRepository repository) {
        this.repository = repository;
    }

    public List<WordEntity> getWordsDueToday() {
        return repository.getWordsDueForReviewSync();
    }
}
package com.example.lenslingo.ui.stats;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lenslingo.data.model.StatsData;
import com.example.lenslingo.repository.StatsRepository;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsViewModel extends AndroidViewModel {

    private final StatsRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<StatsData> stats = new MutableLiveData<>();

    public StatsViewModel(@NonNull Application application) {
        super(application);
        repository = new StatsRepository(application);
    }

    public LiveData<StatsData> getStats() {
        return stats;
    }

    private Locale locale = Locale.getDefault();

    public void load(Locale locale) {
        this.locale = locale;
        executor.execute(() -> stats.postValue(repository.loadStats(locale)));
    }

    public void seedDemo() {
        final Locale l = locale;
        repository.seedDemoData();
        executor.execute(() -> {
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            stats.postValue(repository.loadStats(l));
        });
    }
}
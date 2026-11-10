package com.example.lenslingo.repository;

import android.app.Application;

import com.example.lenslingo.data.local.AppDatabase;
import com.example.lenslingo.data.local.DayCount;
import com.example.lenslingo.data.local.ReviewDao;
import com.example.lenslingo.data.local.ReviewLogEntity;
import com.example.lenslingo.data.local.WordDao;
import com.example.lenslingo.data.model.StatsData;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsRepository {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final int LEARNED_MIN_REPETITIONS = 1;

    private final WordDao wordDao;
    private final ReviewDao reviewDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public StatsRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        wordDao = db.wordDao();
        reviewDao = db.reviewDao();
    }

    public void logReview(int wordId, int quality) {
        executor.execute(() ->
                reviewDao.insert(new ReviewLogEntity(wordId, System.currentTimeMillis(), quality)));
    }

    // Блокуючий метод: викликати лише з фонового потоку
    public StatsData loadStats() {
        long now = System.currentTimeMillis();
        long offset = TimeZone.getDefault().getOffset(now);
        long today = (now + offset) / DAY;

        StatsData data = new StatsData();
        data.total = wordDao.countAllSync();
        data.learned = wordDao.countLearnedSync(LEARNED_MIN_REPETITIONS);
        data.languages = wordDao.languageBreakdown();

        List<Long> days = reviewDao.distinctDays(offset);
        if (!days.isEmpty()) {
            data.daysSinceLast = (int) (today - days.get(0));
            if (data.daysSinceLast <= 1) {
                data.streak = 1;
                for (int i = 1; i < days.size(); i++) {
                    if (days.get(i) == days.get(i - 1) - 1) data.streak++;
                    else break;
                }
            }
        }

        long since = (today - 6) * DAY - offset;
        Map<Long, Integer> counts = new HashMap<>();
        for (DayCount dc : reviewDao.countsSince(offset, since)) counts.put(dc.day, dc.cnt);

        SimpleDateFormat fmt = new SimpleDateFormat("EEE", Locale.ENGLISH);
        data.dayLabels = new String[7];
        data.dayCounts = new int[7];
        for (int i = 0; i < 7; i++) {
            long dayNumber = today - 6 + i;
            Integer c = counts.get(dayNumber);
            data.dayCounts[i] = c == null ? 0 : c;
            data.dayLabels[i] = fmt.format(now - (6 - i) * DAY);
        }
        return data;
    }

    // Лише для тестування та скріншотів: видаліть перед здачею
    public void seedDemoData() {
        executor.execute(() -> {
            Random rnd = new Random();
            long now = System.currentTimeMillis();
            for (int d = 0; d < 5; d++) {
                int n = 3 + rnd.nextInt(8);
                for (int i = 0; i < n; i++) {
                    reviewDao.insert(new ReviewLogEntity(
                            0, now - d * DAY - rnd.nextInt(3_600_000), 3 + rnd.nextInt(3)));
                }
            }
        });
    }
}
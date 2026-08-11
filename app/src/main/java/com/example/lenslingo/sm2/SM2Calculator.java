package com.example.lenslingo.sm2;

import java.util.concurrent.TimeUnit;

public class SM2Calculator {

    private static final float MIN_EASE_FACTOR = 1.3f;
    private static final int PASS_THRESHOLD = 3;

    public SM2Result calculate(int quality, float currentEaseFactor,
                               int currentRepetitionCount, int currentIntervalDays) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5");
        }

        float newEaseFactor = currentEaseFactor
                + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f));
        if (newEaseFactor < MIN_EASE_FACTOR) {
            newEaseFactor = MIN_EASE_FACTOR;
        }

        int newRepetitionCount;
        int newIntervalDays;

        if (quality < PASS_THRESHOLD) {
            newRepetitionCount = 0;
            newIntervalDays = 1;
        } else {
            newRepetitionCount = currentRepetitionCount + 1;

            if (newRepetitionCount == 1) {
                newIntervalDays = 1;
            } else if (newRepetitionCount == 2) {
                newIntervalDays = 6;
            } else {
                newIntervalDays = Math.round(currentIntervalDays * newEaseFactor);
            }
        }

        long nextReviewDate = System.currentTimeMillis()
                + TimeUnit.DAYS.toMillis(newIntervalDays);

        return new SM2Result(newEaseFactor, newRepetitionCount, newIntervalDays, nextReviewDate);
    }
}
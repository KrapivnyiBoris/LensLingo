package com.example.lenslingo.sm2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class SM2CalculatorTest {

    private static final float DELTA = 0.001f;
    private SM2Calculator calculator;

    @Before
    public void setUp() {
        calculator = new SM2Calculator();
    }

    // T1: перше успішне повторення дає інтервал 1 день
    @Test
    public void firstSuccessfulReview_givesOneDay() {
        SM2Result r = calculator.calculate(5, 2.5f, 0, 0);
        assertEquals(1, r.repetitionCount);
        assertEquals(1, r.intervalDays);
        assertEquals(2.6f, r.easeFactor, DELTA);
    }

    // T2: друге успішне повторення дає інтервал 6 днів
    @Test
    public void secondSuccessfulReview_givesSixDays() {
        SM2Result r = calculator.calculate(5, 2.6f, 1, 1);
        assertEquals(2, r.repetitionCount);
        assertEquals(6, r.intervalDays);
    }

    // T3: далі інтервал = попередній інтервал × оновлений EF (6 × 2.8 = 16.8 → 17)
    @Test
    public void thirdSuccessfulReview_multipliesByEaseFactor() {
        SM2Result r = calculator.calculate(5, 2.7f, 2, 6);
        assertEquals(3, r.repetitionCount);
        assertEquals(2.8f, r.easeFactor, DELTA);
        assertEquals(17, r.intervalDays);
    }

    // T4: повна послідовність п'яти відповідей «5» підряд
    @Test
    public void sequenceOfPerfectAnswers_matchesManualCalculation() {
        float ef = 2.5f;
        int n = 0;
        int interval = 0;
        int[] expectedIntervals = {1, 6, 17, 49, 147};

        for (int step = 0; step < expectedIntervals.length; step++) {
            SM2Result r = calculator.calculate(5, ef, n, interval);
            assertEquals("step " + (step + 1), expectedIntervals[step], r.intervalDays);
            ef = r.easeFactor;
            n = r.repetitionCount;
            interval = r.intervalDays;
        }
    }

    // T5: оцінка нижче 3 скидає прогрес незалежно від попереднього
    @Test
    public void failedReview_resetsProgress() {
        SM2Result r = calculator.calculate(2, 2.5f, 5, 30);
        assertEquals(0, r.repetitionCount);
        assertEquals(1, r.intervalDays);
        assertEquals(2.18f, r.easeFactor, DELTA);
    }

    // T6: межа успіху: 3 це ще успіх, 2 це вже провал
    @Test
    public void thresholdBoundary_threePassesTwoFails() {
        SM2Result pass = calculator.calculate(3, 2.5f, 0, 0);
        SM2Result fail = calculator.calculate(2, 2.5f, 0, 0);
        assertEquals(1, pass.repetitionCount);
        assertEquals(0, fail.repetitionCount);
    }

    // T7: зміна EF для кожної оцінки 0-5 за формулою (1.1)
    @Test
    public void easeFactorChange_matchesFormulaForEveryQuality() {
        float[] expectedDelta = {-0.80f, -0.54f, -0.32f, -0.14f, 0.00f, 0.10f};
        for (int q = 0; q <= 5; q++) {
            SM2Result r = calculator.calculate(q, 2.5f, 0, 0);
            assertEquals("quality " + q, 2.5f + expectedDelta[q], r.easeFactor, DELTA);
        }
    }

    // T8: EF ніколи не опускається нижче 1.3
    @Test
    public void easeFactor_neverDropsBelowMinimum() {
        assertEquals(1.3f, calculator.calculate(0, 1.3f, 3, 10).easeFactor, DELTA);

        float ef = 1.5f;
        for (int i = 0; i < 20; i++) {
            ef = calculator.calculate(3, ef, 4, 10).easeFactor;
            assertTrue("EF " + ef, ef >= 1.3f - DELTA);
        }
    }

    // T9: оцінка поза діапазоном 0-5 відхиляється
    @Test(expected = IllegalArgumentException.class)
    public void qualityAboveFive_isRejected() {
        calculator.calculate(6, 2.5f, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeQuality_isRejected() {
        calculator.calculate(-1, 2.5f, 0, 0);
    }

    // T10: дата наступного повторення = зараз + інтервал у днях
    @Test
    public void nextReviewDate_isNowPlusInterval() {
        long before = System.currentTimeMillis();
        SM2Result r = calculator.calculate(5, 2.5f, 1, 1);
        long after = System.currentTimeMillis();

        long days = TimeUnit.DAYS.toMillis(r.intervalDays);
        assertTrue(r.nextReviewDate >= before + days);
        assertTrue(r.nextReviewDate <= after + days);
    }
}
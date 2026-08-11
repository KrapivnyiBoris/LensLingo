package com.example.lenslingo.sm2;

public class SM2Result {
    public final float easeFactor;
    public final int repetitionCount;
    public final int intervalDays;
    public final long nextReviewDate;

    public SM2Result(float easeFactor, int repetitionCount, int intervalDays, long nextReviewDate) {
        this.easeFactor = easeFactor;
        this.repetitionCount = repetitionCount;
        this.intervalDays = intervalDays;
        this.nextReviewDate = nextReviewDate;
    }
}
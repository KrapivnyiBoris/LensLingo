package com.example.lenslingo.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "words",
        indices = {@Index(value = {"word", "sourceLanguage"}, unique = true)}
)
public class WordEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String word;

    @NonNull
    public String translation;

    @NonNull
    public String sourceLanguage;

    public String photoUri;

    public long dateAdded;

    public float easeFactor = 2.5f;
    public int repetitionCount = 0;
    public int intervalDays = 0;
    public long nextReviewDate;
}
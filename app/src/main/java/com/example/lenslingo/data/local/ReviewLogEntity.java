package com.example.lenslingo.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "review_log", indices = {@Index("reviewedAt")})
public class ReviewLogEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int wordId;
    public long reviewedAt;
    public int quality;

    public ReviewLogEntity(int wordId, long reviewedAt, int quality) {
        this.wordId = wordId;
        this.reviewedAt = reviewedAt;
        this.quality = quality;
    }
}
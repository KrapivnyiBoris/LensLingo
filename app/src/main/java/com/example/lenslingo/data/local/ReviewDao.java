package com.example.lenslingo.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReviewDao {

    @Insert
    void insert(ReviewLogEntity log);

    @Query("SELECT DISTINCT (reviewedAt + :offset) / 86400000 AS day "
            + "FROM review_log ORDER BY day DESC")
    List<Long> distinctDays(long offset);

    @Query("SELECT (reviewedAt + :offset) / 86400000 AS day, COUNT(*) AS cnt "
            + "FROM review_log WHERE reviewedAt >= :since GROUP BY day")
    List<DayCount> countsSince(long offset, long since);
}
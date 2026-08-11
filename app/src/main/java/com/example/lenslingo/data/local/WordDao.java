package com.example.lenslingo.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(WordEntity word);

    @Update
    void update(WordEntity word);

    @Delete
    void delete(WordEntity word);

    @Query("SELECT * FROM words ORDER BY dateAdded DESC")
    LiveData<List<WordEntity>> getAllWords();

    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' ORDER BY dateAdded DESC")
    LiveData<List<WordEntity>> searchWords(String query);

    @Query("SELECT * FROM words WHERE nextReviewDate <= :now")
    List<WordEntity> getWordsDueForReview(long now);

    @Query("SELECT COUNT(*) FROM words")
    LiveData<Integer> getTotalWordsCount();
}

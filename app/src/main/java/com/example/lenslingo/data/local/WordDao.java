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

    @Query("SELECT COUNT(*) FROM words")
    int countAllSync();

    @Query("SELECT COUNT(*) FROM words WHERE repetitionCount >= :min")
    int countLearnedSync(int min);

    @Query("SELECT sourceLanguage AS language, COUNT(*) AS cnt FROM words "
            + "GROUP BY sourceLanguage ORDER BY cnt DESC")
    List<LanguageCount> languageBreakdown();

    @Query("SELECT COUNT(*) FROM words WHERE photoUri = :uri")
    int countByPhotoSync(String uri);

    @Query("SELECT * FROM words WHERE id = :id")
    WordEntity getByIdSync(int id);

    @Query("UPDATE words SET translation = :translation WHERE id = :id")
    void updateTranslation(int id, String translation);

    @Query("SELECT word, translation FROM words WHERE sourceLanguage = :lang")
    List<GlossaryEntry> glossary(String lang);
}

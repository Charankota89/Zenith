package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.zenith.app.data.db.entity.MoodEntryEntity;

import java.util.List;

@Dao
public interface MoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MoodEntryEntity entry);

    /** Get mood for a specific date. */
    @Query("SELECT * FROM mood_entry WHERE date = :date LIMIT 1")
    LiveData<MoodEntryEntity> getMoodForDate(String date);

    /** Non-LiveData version to check if mood is already recorded today. */
    @Query("SELECT * FROM mood_entry WHERE date = :date LIMIT 1")
    MoodEntryEntity getMoodForDateSync(String date);

    /** Get last 7 days of mood for the chart. */
    @Query("SELECT * FROM mood_entry WHERE date >= :fromDate ORDER BY date ASC")
    LiveData<List<MoodEntryEntity>> getMoodSince(String fromDate);

    /** Weekly mood data for wellness report. */
    @Query("SELECT * FROM mood_entry WHERE date >= :fromDate ORDER BY date ASC")
    List<MoodEntryEntity> getMoodSinceSync(String fromDate);

    /** Delete old mood entries. */
    @Query("DELETE FROM mood_entry WHERE date < :cutoff")
    void deleteOlderThan(String cutoff);
}

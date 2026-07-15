package com.zenith.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.zenith.app.db.entity.HabitEntity;
import java.util.List;

@Dao
public interface HabitDao {
    @Insert
    void insert(HabitEntity habit);

    @Update
    void update(HabitEntity habit);

    @Delete
    void delete(HabitEntity habit);

    @Query("SELECT * FROM habits")
    LiveData<List<HabitEntity>> getAllHabits();

    @Query("SELECT * FROM habits")
    List<HabitEntity> getAllHabitsSync();

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    HabitEntity getHabitById(int id);

    @Query("UPDATE habits SET completedToday = 0")
    void resetDailyCompletion();

    @Query("SELECT COUNT(*) FROM habits WHERE completedToday = 1")
    int countCompletedToday();

    // Reactive variant for the Home dashboard, so the habit count updates
    // live as habits are checked off elsewhere in the app.
    @Query("SELECT COUNT(*) FROM habits WHERE completedToday = 1")
    LiveData<Integer> observeCompletedCountToday();

    @Query("SELECT COUNT(*) FROM habits WHERE completedToday = 1 AND :date IS NOT NULL")
    int getCompletedCountForDate(String date);

    @Query("SELECT COUNT(*) FROM habits")
    int getTotalCount();

    @Query("SELECT * FROM habits WHERE lastCompletedDate = :date")
    List<HabitEntity> getHabitsCompletedOnDateSync(String date);
}

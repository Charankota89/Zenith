package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.zenith.app.data.db.entity.HabitEntity;
import com.zenith.app.data.db.entity.HabitLogEntity;

import java.util.List;

@Dao
public interface HabitDao {

    // ─── Habit definitions ───────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertHabit(HabitEntity habit);

    @Update
    void updateHabit(HabitEntity habit);

    @Query("SELECT * FROM habit WHERE isActive = 1 ORDER BY habitName ASC")
    LiveData<List<HabitEntity>> getActiveHabits();

    @Query("SELECT * FROM habit ORDER BY habitName ASC")
    LiveData<List<HabitEntity>> getAllHabits();

    @Query("SELECT * FROM habit WHERE id = :id")
    HabitEntity getHabitByIdSync(long id);

    @Query("UPDATE habit SET currentStreak = :streak, longestStreak = :longest WHERE id = :id")
    void updateStreak(long id, int streak, int longest);

    @Query("DELETE FROM habit WHERE id = :id")
    void deleteHabit(long id);

    // ─── Habit logs ──────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertLog(HabitLogEntity log);

    /** All logs for a specific date. */
    @Query("SELECT * FROM habit_log WHERE completedDate = :date")
    LiveData<List<HabitLogEntity>> getLogsForDate(String date);

    /** Check if a habit was completed on a specific date (non-LiveData). */
    @Query("SELECT completed FROM habit_log WHERE habitId = :habitId AND completedDate = :date")
    Boolean isCompletedSync(long habitId, String date);

    /** Number of completed habits on a given date. */
    @Query("SELECT COUNT(*) FROM habit_log WHERE completedDate = :date AND completed = 1")
    int getCompletedCountSync(String date);

    /** Get last 7 days of logs for a habit (for streak calculation). */
    @Query("SELECT * FROM habit_log WHERE habitId = :habitId AND completedDate >= :fromDate " +
           "ORDER BY completedDate DESC")
    List<HabitLogEntity> getRecentLogsSync(long habitId, String fromDate);
}

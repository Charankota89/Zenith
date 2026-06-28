package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.zenith.app.data.db.entity.FocusSessionEntity;

import java.util.List;

@Dao
public interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FocusSessionEntity entity);

    @Update
    void update(FocusSessionEntity entity);

    /** All sessions for a given date. */
    @Query("SELECT * FROM focus_session WHERE date = :date ORDER BY startTime DESC")
    LiveData<List<FocusSessionEntity>> getSessionsForDate(String date);

    /** Total minutes studied on a given date. */
    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM focus_session WHERE date = :date")
    LiveData<Integer> getTotalMinutesForDate(String date);

    /** Non-LiveData version (for WorkManager / goal checking). */
    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM focus_session WHERE date = :date")
    int getTotalMinutesForDateSync(String date);

    /** Weekly total grouped by date. */
    @Query("SELECT date, SUM(durationMinutes) as totalMinutes FROM focus_session " +
           "WHERE date >= :fromDate GROUP BY date ORDER BY date ASC")
    LiveData<List<DailyStudyTotal>> getWeeklyStudyTotals(String fromDate);

    /** Get the currently active (ongoing) session. */
    @Query("SELECT * FROM focus_session WHERE endTime = 0 LIMIT 1")
    FocusSessionEntity getActiveSessionSync();

    /** End an active session by setting endTime and duration. */
    @Query("UPDATE focus_session SET endTime = :endTime, durationMinutes = :duration WHERE id = :id")
    void endSession(long id, long endTime, long duration);

    /** Delete sessions older than 90 days. */
    @Query("DELETE FROM focus_session WHERE date < :cutoff")
    void deleteOlderThan(String cutoff);

    // Projection
    class DailyStudyTotal {
        public String date;
        public long totalMinutes;
    }
}

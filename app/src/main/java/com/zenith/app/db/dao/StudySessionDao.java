package com.zenith.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.zenith.app.db.entity.StudySessionEntity;
import java.util.List;

@Dao
public interface StudySessionDao {
    @Insert
    void insert(StudySessionEntity session);

    @Query("SELECT * FROM study_sessions WHERE date = :date")
    LiveData<List<StudySessionEntity>> getSessionsForDate(String date);

    @Query("SELECT SUM(durationMillis) FROM study_sessions WHERE date = :date")
    long getTotalStudyTimeForDate(String date);

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC LIMIT 30")
    LiveData<List<StudySessionEntity>> getRecentSessions();

    @Query("SELECT SUM(durationMillis) FROM study_sessions WHERE date >= :fromDate")
    long getTotalStudyTimeSince(String fromDate);

    @Query("SELECT COUNT(*) FROM study_sessions WHERE date = :date")
    int getSessionCountForDate(String date);

    @Query("SELECT * FROM study_sessions WHERE date = :date")
    List<StudySessionEntity> getSessionsForDateSync(String date);
}

package com.zenith.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.zenith.app.db.entity.PomodoroEntity;
import java.util.List;

@Dao
public interface PomodoroDao {
    @Insert
    void insert(PomodoroEntity entity);

    @Update
    void update(PomodoroEntity entity);

    @Query("SELECT * FROM pomodoro_log WHERE date = :date LIMIT 1")
    PomodoroEntity getPomodoroForDate(String date);

    @Query("SELECT * FROM pomodoro_log ORDER BY date DESC LIMIT 7")
    LiveData<List<PomodoroEntity>> getRecentPomodoros();
}

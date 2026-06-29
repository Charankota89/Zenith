package com.zenith.app.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pomodoro_log")
public class PomodoroEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int    sessionsCompleted;
    public long   totalFocusMillis;
    public String date;
}

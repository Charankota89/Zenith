package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Stores each focus/study session logged when Focus Mode is active.
 */
@Entity(tableName = "focus_session")
public class FocusSessionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Subject tag e.g. "DSA", "Java", "General Study" */
    public String subject;

    /** Epoch millis when session started */
    public long startTime;

    /** Epoch millis when session ended (0 if ongoing) */
    public long endTime;

    /** Duration in minutes (endTime - startTime) */
    public long durationMinutes;

    /** Date yyyy-MM-dd */
    public String date;

    /** Whether session was pomodoro-assisted */
    public boolean wasPomodoro;

    public FocusSessionEntity() {}

    public FocusSessionEntity(String subject, long startTime, String date) {
        this.subject   = subject;
        this.startTime = startTime;
        this.endTime   = 0;
        this.date      = date;
        this.wasPomodoro = false;
    }
}

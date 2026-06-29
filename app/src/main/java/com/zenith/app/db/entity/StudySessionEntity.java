package com.zenith.app.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "study_sessions")
public class StudySessionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String subject;
    public long   durationMillis;
    public long   startTime;
    public long   endTime;
    public String date;
}

package com.zenith.app.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mood_log")
public class MoodEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int    moodScore;
    public String date;
    public String note;
}

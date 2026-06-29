package com.zenith.app.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habits")
public class HabitEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String  habitName;
    public int     currentStreak;
    public int     longestStreak;
    public String  lastCompletedDate;
    public boolean completedToday;
}

package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * User-defined daily habit (e.g. "Solve 1 LeetCode", "Read 20 pages").
 */
@Entity(tableName = "habit")
public class HabitEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Habit description */
    public String habitName;

    /** Icon emoji or resource name */
    public String icon;

    /** Whether habit is active */
    public boolean isActive;

    /** Epoch millis when created */
    public long createdAt;

    /** Current streak count */
    public int currentStreak;

    /** Longest streak ever achieved */
    public int longestStreak;

    public HabitEntity() {}

    public HabitEntity(String habitName, String icon) {
        this.habitName     = habitName;
        this.icon          = icon;
        this.isActive      = true;
        this.createdAt     = System.currentTimeMillis();
        this.currentStreak = 0;
        this.longestStreak = 0;
    }
}

package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tracks XP earned for completing goals, habits, and streaks.
 * Powers the Motivation Engine (Module 5).
 */
@Entity(tableName = "xp_record")
public class XpRecordEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Action that earned XP e.g. "GOAL_MET", "HABIT_COMPLETE", "FOCUS_SESSION" */
    public String action;

    /** XP earned in this action */
    public int xpEarned;

    /** Cumulative total XP (snapshot at time of record) */
    public int totalXp;

    /** User level label e.g. "Beginner", "Focused", "Disciplined", "Champion" */
    public String level;

    /** Date yyyy-MM-dd */
    public String date;

    /** Epoch millis */
    public long earnedAt;

    public XpRecordEntity() {}

    public XpRecordEntity(String action, int xpEarned,
                          int totalXp, String level,
                          String date, long earnedAt) {
        this.action   = action;
        this.xpEarned = xpEarned;
        this.totalXp  = totalXp;
        this.level    = level;
        this.date     = date;
        this.earnedAt = earnedAt;
    }
}

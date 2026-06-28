package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Stores the user's daily study goal and how much has been achieved.
 * One row per date.
 */
@Entity(tableName = "study_goal")
public class StudyGoalEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Target minutes per day set by user */
    public int targetMinutes;

    /** Actual minutes achieved today (sum of FocusSessions) */
    public int achievedMinutes;

    /** Date yyyy-MM-dd */
    public String date;

    /** Whether congratulation notification has been sent */
    public boolean goalMet;
    public boolean notifiedGoalMet;

    public StudyGoalEntity() {}

    public StudyGoalEntity(int targetMinutes, String date) {
        this.targetMinutes      = targetMinutes;
        this.achievedMinutes    = 0;
        this.date               = date;
        this.goalMet            = false;
        this.notifiedGoalMet    = false;
    }
}

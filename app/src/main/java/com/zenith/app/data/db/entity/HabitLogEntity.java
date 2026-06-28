package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

/**
 * Daily completion log for each habit.
 * One row per habit per date.
 */
@Entity(
    tableName = "habit_log",
    foreignKeys = @ForeignKey(
        entity = HabitEntity.class,
        parentColumns = "id",
        childColumns  = "habitId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"habitId", "completedDate"}, unique = true)
    }
)
public class HabitLogEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long habitId;

    /** Date yyyy-MM-dd */
    public String completedDate;

    /** Whether the habit was completed on this date */
    public boolean completed;

    public HabitLogEntity() {}

    public HabitLogEntity(long habitId, String completedDate, boolean completed) {
        this.habitId       = habitId;
        this.completedDate = completedDate;
        this.completed     = completed;
    }
}

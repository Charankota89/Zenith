package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

/**
 * Daily mood check-in entry.
 * One row per date.
 */
@Entity(
    tableName = "mood_entry",
    indices = {@Index(value = "date", unique = true)}
)
public class MoodEntryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 1=Terrible, 2=Bad, 3=Neutral, 4=Good, 5=Amazing */
    public int moodScore;

    /** Label e.g. "😔 Terrible" */
    public String moodLabel;

    /** Optional note from the user */
    public String note;

    /** Date yyyy-MM-dd */
    public String date;

    /** Epoch millis when recorded */
    public long recordedAt;

    public MoodEntryEntity() {}

    public MoodEntryEntity(int moodScore, String moodLabel,
                           String note, String date, long recordedAt) {
        this.moodScore  = moodScore;
        this.moodLabel  = moodLabel;
        this.note       = note;
        this.date       = date;
        this.recordedAt = recordedAt;
    }
}

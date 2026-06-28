package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tracks time logged per skill (e.g. Java, DSA, System Design).
 */
@Entity(tableName = "skill_entry")
public class SkillEntryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** User-defined skill name */
    public String skillName;

    /** Minutes logged in this session */
    public long minutesLogged;

    /** Epoch millis session started */
    public long sessionStart;

    /** Date yyyy-MM-dd */
    public String date;

    public SkillEntryEntity() {}

    public SkillEntryEntity(String skillName, long minutesLogged,
                            long sessionStart, String date) {
        this.skillName    = skillName;
        this.minutesLogged = minutesLogged;
        this.sessionStart = sessionStart;
        this.date         = date;
    }
}

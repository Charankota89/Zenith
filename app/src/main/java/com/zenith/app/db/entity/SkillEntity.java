package com.zenith.app.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "skills")
public class SkillEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String skillName;
    public long   totalMillis;
    public String lastPracticedDate;
}

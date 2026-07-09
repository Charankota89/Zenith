package com.zenith.app.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_usage")
public class AppUsageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String  packageName;
    public String  appName;
    public long    usageTimeMillis;
    public long    limitMillis;
    public boolean isLocked;
    public boolean isFocusWhitelisted;
    public boolean isCareerApp;
    public String  date;
    public long    unlockExpiresAt;
    public String  unlockReason;
}

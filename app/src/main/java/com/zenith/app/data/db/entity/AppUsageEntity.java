package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

/**
 * Stores per-app daily usage pulled from UsageStatsManager.
 * One row per app per day.
 */
@Entity(
    tableName = "app_usage",
    indices = {
        @Index(value = {"packageName", "date"}, unique = true)
    }
)
public class AppUsageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Package name e.g. com.instagram.android */
    public String packageName;

    /** Human-readable app name */
    public String appName;

    /** Total minutes used on this date */
    public long usageMinutes;

    /** Date string in yyyy-MM-dd format */
    public String date;

    /** Timestamp of last update (epoch millis) */
    public long updatedAt;

    public AppUsageEntity() {}

    public AppUsageEntity(String packageName, String appName,
                          long usageMinutes, String date, long updatedAt) {
        this.packageName  = packageName;
        this.appName      = appName;
        this.usageMinutes = usageMinutes;
        this.date         = date;
        this.updatedAt    = updatedAt;
    }
}

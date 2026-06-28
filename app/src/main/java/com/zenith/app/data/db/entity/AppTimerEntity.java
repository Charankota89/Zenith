package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

/**
 * Stores user-configured daily time limits per app.
 * One row per package (updated in place).
 */
@Entity(
    tableName = "app_timer",
    indices = {@Index(value = "packageName", unique = true)}
)
public class AppTimerEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String packageName;
    public String appName;

    /** Daily limit in minutes set by the user */
    public int dailyLimitMinutes;

    /** Whether the app is currently auto-locked due to limit reached */
    public boolean isLocked;

    /** Whether timer/lock is active for this app */
    public boolean isEnabled;

    /** Timestamp when limit was last reset (midnight) */
    public long lastResetAt;

    public AppTimerEntity() {}

    public AppTimerEntity(String packageName, String appName,
                          int dailyLimitMinutes) {
        this.packageName       = packageName;
        this.appName           = appName;
        this.dailyLimitMinutes = dailyLimitMinutes;
        this.isLocked          = false;
        this.isEnabled         = true;
        this.lastResetAt       = System.currentTimeMillis();
    }
}

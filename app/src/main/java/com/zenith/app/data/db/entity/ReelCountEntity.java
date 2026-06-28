package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Stores reel-watching counts per platform per day.
 * Populated by AccessibilityService scroll event tracking.
 */
@Entity(tableName = "reel_count")
public class ReelCountEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** e.g. "Instagram", "YouTube", "TikTok" */
    public String platform;

    /** Package name of the social app */
    public String packageName;

    /** Total reels counted today */
    public int reelCount;

    /** Approximate minutes spent watching reels today */
    public long minutesSpent;

    /** Date yyyy-MM-dd */
    public String date;

    public long updatedAt;

    public ReelCountEntity() {}

    public ReelCountEntity(String platform, String packageName,
                           int reelCount, long minutesSpent,
                           String date, long updatedAt) {
        this.platform    = platform;
        this.packageName = packageName;
        this.reelCount   = reelCount;
        this.minutesSpent = minutesSpent;
        this.date        = date;
        this.updatedAt   = updatedAt;
    }
}

package com.zenith.app.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "browser_visits")
public class BrowserVisitEntity {
    @PrimaryKey(autoGenerate = true)
    public int    id;
    public String browserPackage;  // e.g. com.android.chrome
    public String url;             // full URL or domain
    public String date;            // yyyy-MM-dd
    public long   visitedAt;       // epoch ms
    public int    durationSeconds; // updated on next visit
}

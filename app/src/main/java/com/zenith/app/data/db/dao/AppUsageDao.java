package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.zenith.app.data.db.entity.AppUsageEntity;

import java.util.List;

@Dao
public interface AppUsageDao {

    /**
     * Insert or replace usage record for a package+date combination.
     * Called every time we sync from UsageStatsManager.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AppUsageEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<AppUsageEntity> entities);

    /** Get all apps for a specific date, ordered by usage descending. */
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageMinutes DESC")
    LiveData<List<AppUsageEntity>> getUsageForDate(String date);

    /** Non-LiveData version for background sync. */
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageMinutes DESC")
    List<AppUsageEntity> getUsageForDateSync(String date);

    /** Get usage for a specific package across multiple dates (for weekly chart). */
    @Query("SELECT * FROM app_usage WHERE packageName = :pkg AND date >= :fromDate ORDER BY date ASC")
    LiveData<List<AppUsageEntity>> getUsageForPackageSince(String pkg, String fromDate);

    /** Get top N apps for a given date. */
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageMinutes DESC LIMIT :limit")
    LiveData<List<AppUsageEntity>> getTopAppsForDate(String date, int limit);

    /** Get weekly aggregated usage: sum minutes per package across the last 7 days. */
    @Query("SELECT packageName, appName, SUM(usageMinutes) as usageMinutes, " +
           ":weekLabel as date, MAX(updatedAt) as updatedAt, 0 as id " +
           "FROM app_usage WHERE date >= :fromDate " +
           "GROUP BY packageName ORDER BY usageMinutes DESC LIMIT 10")
    LiveData<List<AppUsageEntity>> getWeeklyTopApps(String fromDate, String weekLabel);

    /** Get daily total screen time for the last 7 days (for bar chart). */
    @Query("SELECT date, SUM(usageMinutes) as totalMinutes FROM app_usage " +
           "WHERE date >= :fromDate GROUP BY date ORDER BY date ASC")
    LiveData<List<DailyTotalUsage>> getDailyTotals(String fromDate);

    /** Get usage minutes for a specific package on a specific date (non-LiveData). */
    @Query("SELECT usageMinutes FROM app_usage WHERE packageName = :pkg AND date = :date")
    long getUsageMinutesSync(String pkg, String date);

    /** Delete records older than 30 days (cleanup). */
    @Query("DELETE FROM app_usage WHERE date < :cutoffDate")
    void deleteOlderThan(String cutoffDate);

    // ─── Projection class for daily total aggregation ───

    class DailyTotalUsage {
        public String date;
        public long totalMinutes;
    }
}

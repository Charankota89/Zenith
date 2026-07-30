package com.zenith.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.zenith.app.db.entity.AppUsageEntity;
import java.util.List;

@Dao
public interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppUsageEntity entity);

    @Update
    void update(AppUsageEntity entity);

    @Query("SELECT id, packageName, appName, MAX(usageTimeMillis) as usageTimeMillis, limitMillis, isLocked, isFocusWhitelisted, isCareerApp, date, unlockExpiresAt, unlockReason FROM app_usage WHERE date = :date GROUP BY packageName ORDER BY usageTimeMillis DESC")
    LiveData<List<AppUsageEntity>> getUsageForDate(String date);

    @Query("SELECT * FROM app_usage WHERE packageName = :pkg AND date = :date ORDER BY id DESC LIMIT 1")
    AppUsageEntity getUsageForApp(String pkg, String date);

    @Query("SELECT * FROM app_usage WHERE date = :date AND isLocked = 1")
    List<AppUsageEntity> getLockedAppsForDate(String date);

    @Query("UPDATE app_usage SET isLocked = 0 WHERE date != :today")
    void unlockAllExceptToday(String today);

    /**
     * Clears lock status and temporary unlock window for today's apps.
     */
    @Query("UPDATE app_usage SET isLocked = 0, unlockExpiresAt = 0 WHERE date = :today")
    void unlockAllForNewDay(String today);

    /**
     * Called by MidnightResetWorker at the start of every new day.
     * Zeroes each app's accumulated usage, clears the lock flag, and
     * clears any temporary unlock window — so the new day starts fresh
     * with 0 ms used rather than carrying yesterday's total forward.
     */
    @Query("UPDATE app_usage SET usageTimeMillis = 0, isLocked = 0, unlockExpiresAt = 0 WHERE date = :today")
    void resetUsageForNewDay(String today);

    /**
     * Returns all rows from the most recent date BEFORE :today that had a
     * non-zero limit set. Used by MidnightResetWorker to copy limits forward
     * into the new day's rows so the user's configured limits persist across
     * midnight instead of vanishing every morning.
     */
    @Query("SELECT * FROM app_usage WHERE date = :yesterday AND limitMillis > 0")
    List<AppUsageEntity> getAppsWithLimitsOnDate(String yesterday);

    @Query("SELECT SUM(usageTimeMillis) FROM app_usage WHERE date = :date")
    long getTotalUsageForDate(String date);

    // Reactive variant used by the Home dashboard so totals update live as
    // the accessibility service records new usage, instead of only showing
    // whatever the total was at the moment the screen was first opened.
    @Query("SELECT SUM(usageTimeMillis) FROM app_usage WHERE date = :date")
    LiveData<Long> observeTotalUsageForDate(String date);

    @Query("SELECT * FROM app_usage WHERE date = :date AND isCareerApp = 1")
    List<AppUsageEntity> getCareerAppsForDate(String date);

    @Query("SELECT id, packageName, appName, MAX(usageTimeMillis) as usageTimeMillis, limitMillis, isLocked, isFocusWhitelisted, isCareerApp, date, unlockExpiresAt, unlockReason FROM app_usage WHERE date = :date GROUP BY packageName ORDER BY usageTimeMillis DESC")
    List<AppUsageEntity> getUsageForDateSync(String date);

    // Weekly trend: total screen time per day, for the last N days starting
    // at :startDate. GROUP BY only returns rows for days that actually have
    // data, so the ViewModel fills in zeros for any missing days.
    @Query("SELECT date, SUM(usageTimeMillis) as totalMillis FROM app_usage " +
           "WHERE date >= :startDate GROUP BY date ORDER BY date ASC")
    LiveData<List<DailyUsageTotal>> observeWeeklyTrend(String startDate);

    public class DailyUsageTotal {
        public String date;
        public long totalMillis;
    }
}

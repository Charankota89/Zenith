package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.zenith.app.data.db.entity.AppTimerEntity;

import java.util.List;

@Dao
public interface AppTimerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AppTimerEntity entity);

    @Update
    void update(AppTimerEntity entity);

    /** All enabled timers, observed by UI. */
    @Query("SELECT * FROM app_timer WHERE isEnabled = 1 ORDER BY appName ASC")
    LiveData<List<AppTimerEntity>> getEnabledTimers();

    /** All timers (including disabled), for management screen. */
    @Query("SELECT * FROM app_timer ORDER BY appName ASC")
    LiveData<List<AppTimerEntity>> getAllTimers();

    /** Get timer for a specific package (called by AccessibilityService). */
    @Query("SELECT * FROM app_timer WHERE packageName = :pkg LIMIT 1")
    AppTimerEntity getTimerForPackageSync(String pkg);

    /** Mark an app as locked (limit reached). */
    @Query("UPDATE app_timer SET isLocked = 1 WHERE packageName = :pkg")
    void lockApp(String pkg);

    /** Unlock a specific app. */
    @Query("UPDATE app_timer SET isLocked = 0 WHERE packageName = :pkg")
    void unlockApp(String pkg);

    /** Reset all locks at midnight. */
    @Query("UPDATE app_timer SET isLocked = 0, lastResetAt = :timestamp")
    void resetAllLocks(long timestamp);

    /** Delete a timer entry. */
    @Query("DELETE FROM app_timer WHERE packageName = :pkg")
    void deleteTimer(String pkg);

    /** Get all locked packages (non-LiveData, for service). */
    @Query("SELECT packageName FROM app_timer WHERE isLocked = 1 AND isEnabled = 1")
    List<String> getLockedPackagesSync();
}

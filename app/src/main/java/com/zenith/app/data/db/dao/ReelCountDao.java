package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.zenith.app.data.db.entity.ReelCountEntity;

import java.util.List;

@Dao
public interface ReelCountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ReelCountEntity entity);

    /** Today's reel counts per platform. */
    @Query("SELECT * FROM reel_count WHERE date = :date ORDER BY reelCount DESC")
    LiveData<List<ReelCountEntity>> getReelCountsForDate(String date);

    /** Non-LiveData: get count for a specific platform today. */
    @Query("SELECT * FROM reel_count WHERE platform = :platform AND date = :date LIMIT 1")
    ReelCountEntity getCountForPlatformSync(String platform, String date);

    /** Increment reel count for a platform. */
    @Query("UPDATE reel_count SET reelCount = reelCount + 1, " +
           "minutesSpent = :minutes, updatedAt = :now " +
           "WHERE platform = :platform AND date = :date")
    void incrementReelCount(String platform, String date, long minutes, long now);

    /** Weekly reel total per platform. */
    @Query("SELECT platform, packageName, SUM(reelCount) as reelCount, " +
           "SUM(minutesSpent) as minutesSpent, :week as date, MAX(updatedAt) as updatedAt, 0 as id " +
           "FROM reel_count WHERE date >= :fromDate GROUP BY platform")
    List<ReelCountEntity> getWeeklyReelSummarySync(String fromDate, String week);

    @Query("DELETE FROM reel_count WHERE date < :cutoff")
    void deleteOlderThan(String cutoff);
}

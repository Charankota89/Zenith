package com.zenith.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.zenith.app.db.entity.BrowserVisitEntity;
import java.util.List;

@Dao
public interface BrowserVisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BrowserVisitEntity visit);

    @Query("SELECT * FROM browser_visits WHERE date = :date ORDER BY visitedAt DESC")
    LiveData<List<BrowserVisitEntity>> getVisitsForDate(String date);

    @Query("SELECT * FROM browser_visits WHERE date = :date ORDER BY visitedAt DESC LIMIT 50")
    List<BrowserVisitEntity> getVisitsForDateSync(String date);

    @Query("SELECT COUNT(*) FROM browser_visits WHERE date = :date")
    int getVisitCountForDate(String date);

    @Query("DELETE FROM browser_visits WHERE date < :cutoffDate")
    void deleteOlderThan(String cutoffDate);
}

package com.zenith.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.zenith.app.data.db.entity.WhitelistAppEntity;

import java.util.List;

@Dao
public interface WhitelistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(WhitelistAppEntity entity);

    @Update
    void update(WhitelistAppEntity entity);

    /** Get all whitelisted apps by type. */
    @Query("SELECT * FROM whitelist_app WHERE type = :type AND isEnabled = 1")
    LiveData<List<WhitelistAppEntity>> getByType(String type);

    /** Non-LiveData: fast lookup by AccessibilityService. */
    @Query("SELECT packageName FROM whitelist_app WHERE type = :type AND isEnabled = 1")
    List<String> getPackagesByTypeSync(String type);

    /** Check if a specific package is whitelisted for a type. */
    @Query("SELECT COUNT(*) FROM whitelist_app WHERE packageName = :pkg AND type = :type AND isEnabled = 1")
    int isWhitelistedSync(String pkg, String type);

    @Query("DELETE FROM whitelist_app WHERE packageName = :pkg AND type = :type")
    void removeFromWhitelist(String pkg, String type);

    @Query("SELECT * FROM whitelist_app ORDER BY type ASC, appName ASC")
    LiveData<List<WhitelistAppEntity>> getAllWhitelistedApps();
}

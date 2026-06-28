package com.zenith.app.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Apps whitelisted for Focus Mode (study) or Career Mode.
 */
@Entity(tableName = "whitelist_app")
public class WhitelistAppEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String packageName;
    public String appName;

    /**
     * Type of whitelist:
     *   "study"  - allowed during Focus Mode
     *   "career" - allowed during Career Mode
     */
    public String type;

    public boolean isEnabled;

    public WhitelistAppEntity() {}

    public WhitelistAppEntity(String packageName, String appName, String type) {
        this.packageName = packageName;
        this.appName     = appName;
        this.type        = type;
        this.isEnabled   = true;
    }
}

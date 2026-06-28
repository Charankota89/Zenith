package com.zenith.app.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.zenith.app.data.db.dao.AppTimerDao;
import com.zenith.app.data.db.dao.AppUsageDao;
import com.zenith.app.data.db.dao.FocusSessionDao;
import com.zenith.app.data.db.dao.HabitDao;
import com.zenith.app.data.db.dao.MoodDao;
import com.zenith.app.data.db.dao.ReelCountDao;
import com.zenith.app.data.db.dao.SkillDao;
import com.zenith.app.data.db.dao.WhitelistDao;
import com.zenith.app.data.db.entity.AppTimerEntity;
import com.zenith.app.data.db.entity.AppUsageEntity;
import com.zenith.app.data.db.entity.FocusSessionEntity;
import com.zenith.app.data.db.entity.HabitEntity;
import com.zenith.app.data.db.entity.HabitLogEntity;
import com.zenith.app.data.db.entity.MoodEntryEntity;
import com.zenith.app.data.db.entity.ReelCountEntity;
import com.zenith.app.data.db.entity.SkillEntryEntity;
import com.zenith.app.data.db.entity.StudyGoalEntity;
import com.zenith.app.data.db.entity.WhitelistAppEntity;
import com.zenith.app.data.db.entity.XpRecordEntity;

/**
 * Zenith Room Database — single source of truth.
 *
 * Version history:
 *  v1 — initial schema (all entities)
 */
@Database(
    entities = {
        AppUsageEntity.class,
        AppTimerEntity.class,
        ReelCountEntity.class,
        FocusSessionEntity.class,
        StudyGoalEntity.class,
        WhitelistAppEntity.class,
        SkillEntryEntity.class,
        HabitEntity.class,
        HabitLogEntity.class,
        MoodEntryEntity.class,
        XpRecordEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class ZenithDatabase extends RoomDatabase {

    private static final String DB_NAME = "zenith_db";
    private static volatile ZenithDatabase INSTANCE;

    // ─── DAOs ───────────────────────────────────────────

    public abstract AppUsageDao    appUsageDao();
    public abstract AppTimerDao    appTimerDao();
    public abstract ReelCountDao   reelCountDao();
    public abstract FocusSessionDao focusSessionDao();
    public abstract HabitDao       habitDao();
    public abstract MoodDao        moodDao();
    public abstract WhitelistDao   whitelistDao();
    public abstract SkillDao       skillDao();

    // ─── Singleton ──────────────────────────────────────

    /**
     * Returns the single shared instance of ZenithDatabase.
     * Thread-safe via double-checked locking.
     */
    public static ZenithDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ZenithDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ZenithDatabase.class,
                            DB_NAME
                        )
                        .fallbackToDestructiveMigration() // acceptable during dev
                        .build();
                }
            }
        }
        return INSTANCE;
    }
}

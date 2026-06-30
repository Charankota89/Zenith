package com.zenith.app.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.zenith.app.db.dao.AppUsageDao;
import com.zenith.app.db.dao.BrowserVisitDao;
import com.zenith.app.db.dao.HabitDao;
import com.zenith.app.db.dao.MoodDao;
import com.zenith.app.db.dao.PomodoroDao;
import com.zenith.app.db.dao.SkillDao;
import com.zenith.app.db.dao.StudySessionDao;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.db.entity.BrowserVisitEntity;
import com.zenith.app.db.entity.HabitEntity;
import com.zenith.app.db.entity.MoodEntity;
import com.zenith.app.db.entity.PomodoroEntity;
import com.zenith.app.db.entity.SkillEntity;
import com.zenith.app.db.entity.StudySessionEntity;

@Database(
    entities = {
        AppUsageEntity.class,
        StudySessionEntity.class,
        HabitEntity.class,
        MoodEntity.class,
        SkillEntity.class,
        PomodoroEntity.class,
        BrowserVisitEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract AppUsageDao     appUsageDao();
    public abstract StudySessionDao studySessionDao();
    public abstract HabitDao        habitDao();
    public abstract MoodDao         moodDao();
    public abstract SkillDao        skillDao();
    public abstract PomodoroDao     pomodoroDao();
    public abstract BrowserVisitDao browserVisitDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "zenith_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}

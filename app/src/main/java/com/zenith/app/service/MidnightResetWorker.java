package com.zenith.app.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.dao.AppUsageDao;
import com.zenith.app.db.dao.HabitDao;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.db.entity.HabitEntity;
import com.zenith.app.util.MidnightScheduler;
import com.zenith.app.util.TimeUtils;
import java.util.List;

public class MidnightResetWorker extends Worker {

    public MidnightResetWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db        = AppDatabase.getInstance(getApplicationContext());
        AppUsageDao usageDao  = db.appUsageDao();
        HabitDao    habitDao  = db.habitDao();
        String      today     = TimeUtils.getTodayDate();
        String      yesterday = TimeUtils.getDateDaysAgo(1);

        // ── Step 1: Zero out today's usage counters ─────────────────────────
        // Without this, GuardianAccessibilityService queries the app_usage row
        // for today and keeps appending to whatever milliseconds were already
        // stored — the user's screen time would never actually reset to 0 at
        // midnight, just keep climbing. This is the primary bug being fixed.
        usageDao.resetUsageForNewDay(today);

        // ── Step 2: Carry limits forward from yesterday ──────────────────────
        // Per-app limits (limitMillis) are written into the app_usage row for
        // a specific date. When GuardianAccessibilityService creates a brand-new
        // row for today (the first time the app is opened), it inserts limitMillis=0
        // unless there's already a row to update. To prevent limits from silently
        // vanishing every morning, copy each yesterday limit into the today row
        // (creating one first if it doesn't exist yet).
        List<AppUsageEntity> appsWithLimits = usageDao.getAppsWithLimitsOnDate(yesterday);
        for (AppUsageEntity yesterdayEntity : appsWithLimits) {
            AppUsageEntity todayEntity = usageDao.getUsageForApp(yesterdayEntity.packageName, today);
            if (todayEntity != null) {
                todayEntity.limitMillis        = yesterdayEntity.limitMillis;
                todayEntity.isCareerApp        = yesterdayEntity.isCareerApp;
                todayEntity.isFocusWhitelisted = yesterdayEntity.isFocusWhitelisted;
                usageDao.update(todayEntity);
            } else {
                AppUsageEntity seed = new AppUsageEntity();
                seed.packageName        = yesterdayEntity.packageName;
                seed.appName            = yesterdayEntity.appName;
                seed.usageTimeMillis    = 0;
                seed.limitMillis        = yesterdayEntity.limitMillis;
                seed.isLocked           = false;
                seed.isFocusWhitelisted = yesterdayEntity.isFocusWhitelisted;
                seed.isCareerApp        = yesterdayEntity.isCareerApp;
                seed.date               = today;
                usageDao.insert(seed);
            }
        }

        // ── Step 3: Unlock all old-day rows ─────────────────────────────────
        usageDao.unlockAllExceptToday(today);

        // ── Step 4: Streak-break check + reset habit completion ──────────────
        // resetDailyCompletion() zeroes completedToday for all habits.
        // But BEFORE clearing the flag, check each habit's lastCompletedDate:
        //   - If lastCompletedDate == yesterday → the user completed it yesterday,
        //     streak is intact — don't break it.
        //   - If lastCompletedDate != yesterday (or null) → the user skipped a day,
        //     the streak is broken — reset currentStreak to 0.
        // This is what makes streaks meaningful: they break on missed days.
        List<HabitEntity> allHabits = habitDao.getAllHabitsSync();
        for (HabitEntity habit : allHabits) {
            boolean completedYesterday = yesterday.equals(habit.lastCompletedDate);
            if (!completedYesterday && habit.currentStreak > 0) {
                // Missed a day — break the streak.
                habit.currentStreak = 0;
                habitDao.update(habit);
            }
        }

        // Now safe to reset today's completion flags.
        habitDao.resetDailyCompletion();

        // ── Step 5: Schedule tomorrow's reset ────────────────────────────────
        MidnightScheduler.scheduleNext(getApplicationContext());

        return Result.success();
    }
}

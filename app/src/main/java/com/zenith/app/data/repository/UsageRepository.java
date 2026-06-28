package com.zenith.app.data.repository;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.zenith.app.data.db.ZenithDatabase;
import com.zenith.app.data.db.dao.AppUsageDao;
import com.zenith.app.data.db.entity.AppUsageEntity;
import com.zenith.app.module1_screen.usage.UsageStatsHelper;
import com.zenith.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for app usage data.
 * Fetches from UsageStatsManager and persists to Room DB.
 * ViewModel talks only to this class — never to DB or system APIs directly.
 */
public class UsageRepository {

    private final AppUsageDao usageDao;
    private final UsageStatsHelper statsHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UsageRepository(Context context) {
        ZenithDatabase db = ZenithDatabase.getInstance(context);
        this.usageDao    = db.appUsageDao();
        this.statsHelper = new UsageStatsHelper(context);
    }

    // ─── Permission check ────────────────────────────────────

    public boolean hasUsagePermission() {
        return statsHelper.hasUsagePermission();
    }

    // ─── Sync from system ────────────────────────────────────

    /**
     * Syncs today's usage from UsageStatsManager → Room DB.
     * Call from WorkManager or ViewModel on a background thread.
     */
    public void syncTodayUsage() {
        executor.execute(() -> {
            List<UsageStatsHelper.AppUsageStat> stats = statsHelper.getTodayUsage();
            String today = DateUtils.today();
            long now = System.currentTimeMillis();

            List<AppUsageEntity> entities = new ArrayList<>();
            for (UsageStatsHelper.AppUsageStat stat : stats) {
                entities.add(new AppUsageEntity(
                        stat.packageName,
                        stat.appName,
                        stat.usageMinutes,
                        today,
                        now
                ));
            }

            if (!entities.isEmpty()) {
                usageDao.upsertAll(entities);
            }
        });
    }

    // ─── LiveData for UI ─────────────────────────────────────

    /**
     * Observe today's app usage, ordered by minutes descending.
     */
    public LiveData<List<AppUsageEntity>> getTodayUsageLive() {
        return usageDao.getUsageForDate(DateUtils.today());
    }

    /**
     * Top N apps today (for dashboard card).
     */
    public LiveData<List<AppUsageEntity>> getTopAppsToday(int limit) {
        return usageDao.getTopAppsForDate(DateUtils.today(), limit);
    }

    /**
     * Daily total screen time for the last 7 days (for bar chart).
     */
    public LiveData<List<AppUsageDao.DailyTotalUsage>> getWeeklyDailyTotals() {
        return usageDao.getDailyTotals(DateUtils.daysAgo(6));
    }

    /**
     * Weekly top apps (aggregate of 7 days) for the weekly chart.
     */
    public LiveData<List<AppUsageEntity>> getWeeklyTopApps() {
        return usageDao.getWeeklyTopApps(DateUtils.daysAgo(6), "week");
    }

    /**
     * Usage minutes for a specific package today (for timer enforcement).
     */
    public long getUsageMinutesForPackageToday(String pkg) {
        // Try Room first (fast), fall back to live system query
        long dbMinutes = usageDao.getUsageMinutesSync(pkg, DateUtils.today());
        if (dbMinutes > 0) return dbMinutes;
        return statsHelper.getUsageMinutesForPackageToday(pkg);
    }

    // ─── Cleanup ─────────────────────────────────────────────

    public void deleteOldRecords() {
        executor.execute(() ->
            usageDao.deleteOlderThan(DateUtils.daysAgo(30))
        );
    }
}

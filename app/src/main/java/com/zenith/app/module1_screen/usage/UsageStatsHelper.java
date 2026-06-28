package com.zenith.app.module1_screen.usage;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import com.zenith.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Wraps Android's UsageStatsManager to pull per-app screen time.
 *
 * ─── PERMISSION REQUIRED ───────────────────────────────────
 * PACKAGE_USAGE_STATS is a "special" permission — it cannot be granted
 * via the normal runtime flow. Grant it with:
 *
 *   adb shell appops set com.zenith.app PACKAGE_USAGE_STATS allow
 *
 * Or guide the user to:
 *   Settings → Apps → Special app access → Usage access → Zenith → Allow
 * ──────────────────────────────────────────────────────────
 */
public class UsageStatsHelper {

    private static final String TAG = "UsageStatsHelper";

    private final Context context;
    private final UsageStatsManager usageStatsManager;
    private final PackageManager packageManager;

    public UsageStatsHelper(Context context) {
        this.context          = context.getApplicationContext();
        this.usageStatsManager = (UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.packageManager   = context.getPackageManager();
    }

    // ─── Permission check ────────────────────────────────────

    /**
     * Returns true if the app has been granted PACKAGE_USAGE_STATS permission.
     * Use this before calling any query methods.
     */
    public boolean hasUsagePermission() {
        AppOpsManager appOps = (AppOpsManager)
                context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // ─── Daily usage ─────────────────────────────────────────

    /**
     * Returns a list of AppUsageStat objects for today, sorted by time descending.
     * Filters out system apps (unless they have a launcher icon).
     */
    public List<AppUsageStat> getTodayUsage() {
        long startOfDay = DateUtils.startOfToday();
        long now        = System.currentTimeMillis();
        return queryUsage(startOfDay, now);
    }

    /**
     * Returns usage for a specific date (yyyy-MM-dd).
     */
    public List<AppUsageStat> getUsageForDate(String date) {
        long[] range = dateToRange(date);
        return queryUsage(range[0], range[1]);
    }

    /**
     * Returns per-day usage totals (in minutes) for the last 7 days.
     * Returns a list of 7 AppUsageStat objects, each with totalMinutes for that day.
     */
    public List<DailyTotal> getWeeklyDailyTotals() {
        List<DailyTotal> totals = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, -i);
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);
            long dayStart = dayCal.getTimeInMillis();

            Calendar dayEnd = (Calendar) dayCal.clone();
            dayEnd.add(Calendar.DAY_OF_YEAR, 1);
            long dayEndMs = Math.min(dayEnd.getTimeInMillis(), System.currentTimeMillis());

            List<AppUsageStat> dayStats = queryUsage(dayStart, dayEndMs);
            long totalMinutes = 0;
            for (AppUsageStat stat : dayStats) {
                totalMinutes += stat.usageMinutes;
            }

            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            totals.add(new DailyTotal(sdf.format(dayCal.getTime()), totalMinutes));
        }
        return totals;
    }

    /**
     * Returns the usage minutes for a specific package today.
     */
    public long getUsageMinutesForPackageToday(String packageName) {
        long start = DateUtils.startOfToday();
        long end   = System.currentTimeMillis();
        Map<String, UsageStats> statsMap =
                usageStatsManager.queryAndAggregateUsageStats(start, end);
        UsageStats stats = statsMap.get(packageName);
        if (stats == null) return 0;
        return stats.getTotalTimeInForeground() / 60_000L;
    }

    // ─── Internal query ──────────────────────────────────────

    private List<AppUsageStat> queryUsage(long beginTime, long endTime) {
        List<AppUsageStat> results = new ArrayList<>();

        if (!hasUsagePermission()) {
            Log.w(TAG, "Usage stats permission not granted");
            return results;
        }

        Map<String, UsageStats> statsMap =
                usageStatsManager.queryAndAggregateUsageStats(beginTime, endTime);

        if (statsMap == null || statsMap.isEmpty()) {
            Log.d(TAG, "No usage stats returned for range");
            return results;
        }

        for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
            String pkg   = entry.getKey();
            long   millis = entry.getValue().getTotalTimeInForeground();

            // Filter: must have at least 1 second of foreground time
            if (millis < 1000) continue;

            // Filter: skip our own app
            if (pkg.equals(context.getPackageName())) continue;

            // Filter: skip system apps without a launcher icon
            if (isSystemApp(pkg) && !hasLauncherIcon(pkg)) continue;

            String appName = getAppName(pkg);
            long usageMinutes = millis / 60_000L;

            results.add(new AppUsageStat(pkg, appName, usageMinutes));
        }

        // Sort by usage descending
        Collections.sort(results,
                (a, b) -> Long.compare(b.usageMinutes, a.usageMinutes));

        return results;
    }

    // ─── Helpers ─────────────────────────────────────────────

    private String getAppName(String pkg) {
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(pkg, 0);
            return (String) packageManager.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private boolean isSystemApp(String pkg) {
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(pkg, 0);
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    private boolean hasLauncherIcon(String pkg) {
        try {
            packageManager.getApplicationIcon(pkg);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private long[] dateToRange(String date) {
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date d = sdf.parse(date);
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(d);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = (Calendar) startCal.clone();
            endCal.add(Calendar.DAY_OF_YEAR, 1);

            return new long[]{
                    startCal.getTimeInMillis(),
                    Math.min(endCal.getTimeInMillis(), System.currentTimeMillis())
            };
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            return new long[]{now - 86_400_000L, now};
        }
    }

    // ─── Data classes ────────────────────────────────────────

    /** Represents per-app usage for a time period. */
    public static class AppUsageStat {
        public final String packageName;
        public final String appName;
        public final long usageMinutes;

        public AppUsageStat(String packageName, String appName, long usageMinutes) {
            this.packageName  = packageName;
            this.appName      = appName;
            this.usageMinutes = usageMinutes;
        }
    }

    /** Represents total screen time for a single day. */
    public static class DailyTotal {
        public final String date;
        public final long totalMinutes;

        public DailyTotal(String date, long totalMinutes) {
            this.date         = date;
            this.totalMinutes = totalMinutes;
        }
    }
}

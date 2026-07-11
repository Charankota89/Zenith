package com.zenith.app.repository;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.TimeUtils;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsageRepository {

    private final Context         context;
    private final AppDatabase     db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UsageRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db      = AppDatabase.getInstance(context);
    }

    public void syncTodayUsage() {
        executor.execute(() -> {
            UsageStatsManager usm = (UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            long now        = System.currentTimeMillis();

            Map<String, UsageStats> statsMap =
                usm.queryAndAggregateUsageStats(startOfDay, now);
            PackageManager pm  = context.getPackageManager();
            String         today = TimeUtils.getTodayDate();

            for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
                String pkg       = entry.getKey();
                long   usageTime = entry.getValue().getTotalTimeInForeground();
                if (usageTime < 1000) continue;

                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                } catch (PackageManager.NameNotFoundException e) { continue; }

                String appName;
                try {
                    appName = pm.getApplicationLabel(
                        pm.getApplicationInfo(pkg, 0)).toString();
                } catch (PackageManager.NameNotFoundException e) { appName = pkg; }

                AppUsageEntity existing = db.appUsageDao().getUsageForApp(pkg, today);
                if (existing != null) {
                    existing.usageTimeMillis = usageTime;
                    db.appUsageDao().update(existing);
                } else {
                    AppUsageEntity entity  = new AppUsageEntity();
                    entity.packageName     = pkg;
                    entity.appName         = appName;
                    entity.usageTimeMillis = usageTime;
                    entity.limitMillis     = 0;
                    entity.isLocked        = false;
                    entity.isFocusWhitelisted = true;
                    entity.date            = today;
                    db.appUsageDao().insert(entity);
                }
            }
        });
    }

    public void setAppLimit(String packageName, long limitMillis) {
        executor.execute(() -> {
            AppUsageEntity entity =
                db.appUsageDao().getUsageForApp(packageName, TimeUtils.getTodayDate());
            if (entity != null) {
                entity.limitMillis = limitMillis;
                db.appUsageDao().update(entity);
            }
        });
    }

    public void toggleCareerApp(String packageName, boolean isCareer) {
        executor.execute(() -> {
            AppUsageEntity entity =
                db.appUsageDao().getUsageForApp(packageName, TimeUtils.getTodayDate());
            if (entity != null) {
                entity.isCareerApp = isCareer;
                db.appUsageDao().update(entity);
            }
        });
    }

    public void toggleWhitelist(String packageName, boolean whitelisted) {
        executor.execute(() -> {
            AppUsageEntity entity =
                db.appUsageDao().getUsageForApp(packageName, TimeUtils.getTodayDate());
            if (entity != null) {
                entity.isFocusWhitelisted = whitelisted;
                db.appUsageDao().update(entity);
            }
        });
    }
}

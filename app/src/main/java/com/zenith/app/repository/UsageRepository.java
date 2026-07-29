package com.zenith.app.repository;

import android.content.Context;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.TimeUtils;
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

    /**
     * Deliberately a no-op now. This used to seed app usage entries from
     * Android's UsageStatsManager — a system API that measures "foreground
     * time" completely differently from, and independently of, the
     * accessibility-service-based tracker that owns this data everywhere
     * else in the app. Two real bugs traced back to this method:
     *   1. It would periodically overwrite the accessibility tracker's
     *      carefully-accumulated (screen-off-aware, lock-aware) numbers
     *      with its own differently-measured total, making the displayed
     *      screen time visibly jump every 5 minutes.
     *   2. Worse: UsageStatsManager can report non-zero "foreground time"
     *      for an app on a brand new day before you've opened anything at
     *      all — a known quirk of how Android buckets usage stats near
     *      day boundaries — which is exactly what showed up as "fake time
     *      at the start of the day."
     * GuardianAccessibilityService already creates a fresh, zeroed entry
     * itself the moment an app is genuinely opened while the screen is on,
     * so there's nothing left for this method to usefully do. Kept as a
     * no-op (rather than deleted outright) so the Settings "Sync" button
     * and UsageMonitorService's screen-on hook don't need to be rewired.
     */
    public void syncTodayUsage() {
        // Intentionally empty.
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

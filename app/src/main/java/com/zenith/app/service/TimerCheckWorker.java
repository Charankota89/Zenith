package com.zenith.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.AppConstants;
import com.zenith.app.util.NotificationHelper;
import com.zenith.app.util.TimeUtils;
import java.util.List;

public class TimerCheckWorker extends Worker {

    // SharedPreferences key prefix for per-app notification dedup.
    // We store the last notified threshold (50 or 80) per packageName per date.
    // Format: "notif_pct_<date>_<packageName>" → 50 or 80
    private static final String PREF_NOTIF_PREFIX = "notif_pct_";

    public TimerCheckWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db    = AppDatabase.getInstance(getApplicationContext());
        String      today = TimeUtils.getTodayDate();
        List<AppUsageEntity> apps = db.appUsageDao().getUsageForDateSync(today);
        if (apps == null) return Result.success();

        SharedPreferences prefs = getApplicationContext()
            .getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (AppUsageEntity e : apps) {
            if (e.limitMillis <= 0 || e.isLocked) continue;
            long pct = (e.usageTimeMillis * 100L) / e.limitMillis;

            // Dedup key: records the highest threshold already notified for
            // this app today so we never fire the same warning twice even if
            // this worker runs every 15 minutes and the usage hasn't changed.
            String prefKey = PREF_NOTIF_PREFIX + today + "_" + e.packageName;
            int lastNotifiedPct = prefs.getInt(prefKey, 0);

            if (pct >= 100) {
                e.isLocked = true;
                db.appUsageDao().update(e);
                // Always notify on lock — lock events are one-shots per day.
                NotificationHelper.notifyLimitWarning(getApplicationContext(), e.appName, 100);
                editor.putInt(prefKey, 100);
            } else if (pct >= 80 && lastNotifiedPct < 80) {
                // Only fire the 80% warning if we haven't already sent it today.
                NotificationHelper.notifyLimitWarning(getApplicationContext(), e.appName, 80);
                editor.putInt(prefKey, 80);
            } else if (pct >= 50 && lastNotifiedPct < 50) {
                // Only fire the 50% warning if we haven't already sent it today.
                NotificationHelper.notifyLimitWarning(getApplicationContext(), e.appName, 50);
                editor.putInt(prefKey, 50);
            }
        }
        editor.apply();
        return Result.success();
    }
}

package com.zenith.app.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.NotificationHelper;
import com.zenith.app.util.TimeUtils;
import java.util.List;

public class TimerCheckWorker extends Worker {

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

        for (AppUsageEntity e : apps) {
            if (e.limitMillis <= 0 || e.isLocked) continue;
            long pct = (e.usageTimeMillis * 100L) / e.limitMillis;
            if (pct >= 100) {
                e.isLocked = true;
                db.appUsageDao().update(e);
                NotificationHelper.notifyLimitWarning(getApplicationContext(), e.appName, 100);
            } else if (pct >= 80) {
                NotificationHelper.notifyLimitWarning(getApplicationContext(), e.appName, 80);
            } else if (pct >= 50) {
                NotificationHelper.notifyLimitWarning(getApplicationContext(), e.appName, 50);
            }
        }
        return Result.success();
    }
}

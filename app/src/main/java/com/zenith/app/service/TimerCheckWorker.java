package com.zenith.app.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.util.AppConstants;
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
        List<AppUsageEntity> apps =
            db.appUsageDao().getUsageForDate(today).getValue();
        if (apps == null) return Result.success();

        for (AppUsageEntity e : apps) {
            if (e.limitMillis <= 0 || e.isLocked) continue;
            long pct = (e.usageTimeMillis * 100L) / e.limitMillis;
            if (pct >= 100) {
                e.isLocked = true;
                db.appUsageDao().update(e);
                notify(e.appName + " is locked for today",
                    "You hit your daily limit. It unlocks tomorrow. Stay on track.");
            } else if (pct >= 80) {
                notify(e.appName + " — 80% of limit used",
                    "Almost at your Zenith limit. Wrap up soon.");
            } else if (pct >= 50) {
                notify(e.appName + " — halfway through limit",
                    "Half your daily limit for " + e.appName + " is gone.");
            }
        }
        return Result.success();
    }

    private void notify(String title, String msg) {
        Intent pi = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent p = PendingIntent.getActivity(getApplicationContext(), 0, pi,
            PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(
            getApplicationContext(), AppConstants.CHANNEL_ID_USAGE)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title).setContentText(msg)
            .setAutoCancel(true).setContentIntent(p);
        ((NotificationManager) getApplicationContext()
            .getSystemService(Context.NOTIFICATION_SERVICE))
            .notify((int) System.currentTimeMillis(), b.build());
    }
}

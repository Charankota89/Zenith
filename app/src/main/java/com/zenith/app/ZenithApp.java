package com.zenith.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.zenith.app.service.EyeBreakWorker;
import com.zenith.app.service.MidnightResetWorker;
import com.zenith.app.service.PostureWorker;
import com.zenith.app.service.TimerCheckWorker;
import com.zenith.app.util.AppConstants;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class ZenithApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        scheduleWorkers();
    }

    private void createNotificationChannels() {
        NotificationManager manager = getSystemService(NotificationManager.class);

        NotificationChannel usageChannel = new NotificationChannel(
            AppConstants.CHANNEL_ID_USAGE, "Usage Alerts",
            NotificationManager.IMPORTANCE_HIGH);
        usageChannel.setDescription("Alerts when you are near your app time limits");

        NotificationChannel focusChannel = new NotificationChannel(
            AppConstants.CHANNEL_ID_FOCUS, "Focus Mode",
            NotificationManager.IMPORTANCE_LOW);
        focusChannel.setDescription("Focus mode active notification");

        NotificationChannel wellbeingChannel = new NotificationChannel(
            AppConstants.CHANNEL_ID_WELLBEING, "Wellbeing Reminders",
            NotificationManager.IMPORTANCE_DEFAULT);
        wellbeingChannel.setDescription("Eye breaks, posture, mood check-ins");

        NotificationChannel careerChannel = new NotificationChannel(
            AppConstants.CHANNEL_ID_CAREER, "Career Updates",
            NotificationManager.IMPORTANCE_DEFAULT);
        careerChannel.setDescription("Career mode and habit streak notifications");

        manager.createNotificationChannel(usageChannel);
        manager.createNotificationChannel(focusChannel);
        manager.createNotificationChannel(wellbeingChannel);
        manager.createNotificationChannel(careerChannel);
    }

    private void scheduleWorkers() {
        WorkManager wm = WorkManager.getInstance(this);

        PeriodicWorkRequest timerCheck = new PeriodicWorkRequest.Builder(
            TimerCheckWorker.class, 15, TimeUnit.MINUTES).build();
        wm.enqueueUniquePeriodicWork("timer_check",
            ExistingPeriodicWorkPolicy.KEEP, timerCheck);

        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_YEAR, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 1);
        midnight.set(Calendar.SECOND, 0);
        long delay = midnight.getTimeInMillis() - System.currentTimeMillis();

        PeriodicWorkRequest midnightReset = new PeriodicWorkRequest.Builder(
            MidnightResetWorker.class, 24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS).build();
        wm.enqueueUniquePeriodicWork("midnight_reset",
            ExistingPeriodicWorkPolicy.KEEP, midnightReset);

        PeriodicWorkRequest eyeBreak = new PeriodicWorkRequest.Builder(
            EyeBreakWorker.class, 20, TimeUnit.MINUTES).build();
        wm.enqueueUniquePeriodicWork("eye_break",
            ExistingPeriodicWorkPolicy.KEEP, eyeBreak);

        PeriodicWorkRequest postureAlert = new PeriodicWorkRequest.Builder(
            PostureWorker.class, 45, TimeUnit.MINUTES).build();
        wm.enqueueUniquePeriodicWork("posture_alert",
            ExistingPeriodicWorkPolicy.KEEP, postureAlert);
    }
}

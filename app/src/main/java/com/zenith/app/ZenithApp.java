package com.zenith.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.zenith.app.service.TimerCheckWorker;
import com.zenith.app.util.AppConstants;
import com.zenith.app.util.MidnightScheduler;
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

        NotificationChannel careerChannel = new NotificationChannel(
            AppConstants.CHANNEL_ID_CAREER, "Career Updates",
            NotificationManager.IMPORTANCE_DEFAULT);
        careerChannel.setDescription("Career mode and habit streak notifications");

        manager.createNotificationChannel(usageChannel);
        manager.createNotificationChannel(focusChannel);
        manager.createNotificationChannel(careerChannel);
    }

    private void scheduleWorkers() {
        WorkManager wm = WorkManager.getInstance(this);

        PeriodicWorkRequest timerCheck = new PeriodicWorkRequest.Builder(
            TimerCheckWorker.class, 15, TimeUnit.MINUTES).build();
        wm.enqueueUniquePeriodicWork("timer_check",
            ExistingPeriodicWorkPolicy.KEEP, timerCheck);

        MidnightScheduler.scheduleNext(this);
    }
}

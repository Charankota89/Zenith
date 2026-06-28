package com.zenith.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.zenith.app.utils.Constants;

/**
 * Application class for Zenith.
 * Initialises notification channels on startup.
 */
public class ZenithApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            // Usage alerts channel
            NotificationChannel usageChannel = new NotificationChannel(
                    Constants.CHANNEL_USAGE_ALERTS,
                    "Usage Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            usageChannel.setDescription("Alerts when you approach or hit your app time limits");

            // Focus channel
            NotificationChannel focusChannel = new NotificationChannel(
                    Constants.CHANNEL_FOCUS,
                    "Focus & Study",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            focusChannel.setDescription("Pomodoro timer, goal completion, and study reminders");

            // Wellness channel
            NotificationChannel wellnessChannel = new NotificationChannel(
                    Constants.CHANNEL_WELLNESS,
                    "Wellness Reminders",
                    NotificationManager.IMPORTANCE_LOW
            );
            wellnessChannel.setDescription("Eye breaks, posture alerts, mood check-ins");

            // Foreground service channel
            NotificationChannel fgChannel = new NotificationChannel(
                    Constants.CHANNEL_FOREGROUND,
                    "Zenith Monitor",
                    NotificationManager.IMPORTANCE_MIN
            );
            fgChannel.setDescription("Background monitoring service");

            nm.createNotificationChannels(java.util.Arrays.asList(
                    usageChannel, focusChannel, wellnessChannel, fgChannel
            ));
        }
    }
}

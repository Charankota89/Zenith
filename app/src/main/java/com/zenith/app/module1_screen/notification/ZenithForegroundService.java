package com.zenith.app.module1_screen.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.zenith.app.R;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.utils.Constants;

/**
 * ZenithForegroundService — persistent background monitor.
 *
 * Feature 4 (NotificationHub) will add:
 *  - 50/80/100% usage alerts per app
 *  - Daily 9 PM summary notification
 *  - Eye break (every 20 min) and posture reminders (every 45 min)
 *
 * For now this is the minimal stub required to satisfy the Manifest
 * and BootReceiver references.
 */
public class ZenithForegroundService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Constants.NOTIF_FOREGROUND_SERVICE, buildNotification());
        return START_STICKY;
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(
                this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, Constants.CHANNEL_FOREGROUND)
                .setContentTitle("Zenith is active")
                .setContentText("Monitoring your screen time in the background")
                .setSmallIcon(R.drawable.ic_zenith_notification)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

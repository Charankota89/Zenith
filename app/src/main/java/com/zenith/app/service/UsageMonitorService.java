package com.zenith.app.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.zenith.app.repository.UsageRepository;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.util.AppConstants;
import java.util.Timer;
import java.util.TimerTask;

public class UsageMonitorService extends Service {

    private Timer           timer;
    private UsageRepository repo;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        repo = new UsageRepository(this);
        startForeground(AppConstants.NOTIF_ID_USAGE_MONITOR, buildNotification());
        startSyncLoop();
        return START_STICKY;
    }

    private void startSyncLoop() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { repo.syncTodayUsage(); }
        }, 0, 5 * 60 * 1000);
    }

    private Notification buildNotification() {
        Intent intent  = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, AppConstants.CHANNEL_ID_FOCUS)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Zenith is active")
            .setContentText("Tracking your screen time")
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}

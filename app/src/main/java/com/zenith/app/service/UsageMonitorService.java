package com.zenith.app.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import com.zenith.app.R;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.repository.UsageRepository;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.util.AppConstants;
import com.zenith.app.util.TimeUtils;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsageMonitorService extends Service {

    // Motivational messages shown on screen unlock
    private static final List<String> MESSAGES = Arrays.asList(
        "Stay focused. Every minute counts. ⚡",
        "You're in control. Make it count. 🎯",
        "Rise above the scroll. 🚀",
        "Your peak is waiting. Keep going. 🏔️",
        "Be intentional. You set these limits. 💪",
        "Less screen, more life. 🌱",
        "Deep work beats distraction. Always. 🧠"
    );

    private Timer             syncTimer;
    private UsageRepository   repo;
    private WindowManager     windowManager;
    private View              screenOnOverlay;
    private BroadcastReceiver screenStateReceiver;
    private final Handler         uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor  = Executors.newSingleThreadExecutor();
    private int messageIndex = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        repo          = new UsageRepository(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        startForeground(AppConstants.NOTIF_ID_USAGE_MONITOR, buildNotification());
        startSyncLoop();
        registerScreenReceiver();
        return START_STICKY;
    }

    // ──────────────────────────────────────────────────
    //  Sync usage every 5 minutes
    // ──────────────────────────────────────────────────
    private void startSyncLoop() {
        syncTimer = new Timer();
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { repo.syncTodayUsage(); }
        }, 0, 5 * 60 * 1000L);
    }

    // ──────────────────────────────────────────────────
    //  Register SCREEN_ON / USER_PRESENT receivers
    //  NOTE: These cannot be declared in Manifest — must be registered dynamically
    // ──────────────────────────────────────────────────
    private void registerScreenReceiver() {
        screenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case Intent.ACTION_SCREEN_ON:
                        // Screen lit up — immediately sync usage
                        repo.syncTodayUsage();
                        break;
                    case Intent.ACTION_USER_PRESENT:
                        // Screen fully unlocked — show summary overlay
                        showScreenOnOverlay();
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                        // Screen turned off — dismiss overlay if still visible
                        removeScreenOnOverlay();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenStateReceiver, filter);
    }

    // ──────────────────────────────────────────────────
    //  Screen-on overlay: summary card on unlock
    // ──────────────────────────────────────────────────
    private void showScreenOnOverlay() {
        executor.execute(() -> {
            AppDatabase db      = AppDatabase.getInstance(getApplicationContext());
            String      today   = TimeUtils.getTodayDate();

            // Get today's screen time total
            long totalMs        = db.appUsageDao().getTotalUsageForDate(today);
            // Get habits done today
            int  habitsDone     = db.habitDao().getCompletedCountForDate(today);
            int  habitsTotal    = db.habitDao().getTotalCount();
            // Get study sessions count (use as focus session count)
            int  studySessions  = db.studySessionDao().getSessionCountForDate(today);

            String screenTime   = TimeUtils.formatDuration(totalMs);
            String habitsText   = habitsDone + " / " + habitsTotal;
            String message      = MESSAGES.get(messageIndex % MESSAGES.size());
            messageIndex++;

            String clockStr     = new SimpleDateFormat("h:mm a", Locale.getDefault())
                                    .format(new Date());

            uiHandler.post(() -> {
                removeScreenOnOverlay();
                LayoutInflater inf = LayoutInflater.from(UsageMonitorService.this);
                screenOnOverlay = inf.inflate(R.layout.overlay_screen_on, null);

                ((TextView) screenOnOverlay.findViewById(R.id.tvScreenOnTime))
                    .setText(clockStr);
                ((TextView) screenOnOverlay.findViewById(R.id.tvScreenOnScreenTime))
                    .setText(screenTime);
                ((TextView) screenOnOverlay.findViewById(R.id.tvScreenOnHabits))
                    .setText(habitsText);
                ((TextView) screenOnOverlay.findViewById(R.id.tvScreenOnFocus))
                    .setText(String.valueOf(studySessions));
                ((TextView) screenOnOverlay.findViewById(R.id.tvScreenOnMessage))
                    .setText(message);

                screenOnOverlay.findViewById(R.id.btnScreenOnClose)
                    .setOnClickListener(v -> removeScreenOnOverlay());

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.TOP;
                params.y = 0;
                windowManager.addView(screenOnOverlay, params);

                // Auto-dismiss after 8 seconds
                uiHandler.postDelayed(this::removeScreenOnOverlay, 8000);
            });
        });
    }

    private void removeScreenOnOverlay() {
        if (screenOnOverlay != null && windowManager != null) {
            try { windowManager.removeView(screenOnOverlay); } catch (Exception ignored) {}
            screenOnOverlay = null;
        }
    }

    // ──────────────────────────────────────────────────
    //  Foreground notification
    // ──────────────────────────────────────────────────
    private Notification buildNotification() {
        Intent      intent = new Intent(this, MainActivity.class);
        PendingIntent pi   = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, AppConstants.CHANNEL_ID_USAGE)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Zenith is active")
            .setContentText("Tracking your screen time · tap to open")
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (syncTimer        != null) syncTimer.cancel();
        if (screenStateReceiver != null) {
            try { unregisterReceiver(screenStateReceiver); } catch (Exception ignored) {}
        }
        executor.shutdown();
        uiHandler.removeCallbacksAndMessages(null);
        removeScreenOnOverlay();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}

package com.zenith.app.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import com.zenith.app.R;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.util.AppConstants;
import com.zenith.app.util.TimeUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GuardianAccessibilityService extends AccessibilityService {

    private WindowManager windowManager;
    private View          lockerOverlay;
    private View          reelPopup;
    private String        currentPkg       = "";
    private int           reelCount        = 0;
    private long          reelSessionStart = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String pkg = event.getPackageName() != null
                ? event.getPackageName().toString() : "";
            if (!pkg.equals(currentPkg)) {
                currentPkg = pkg;
                checkIfLocked(pkg);
                checkFocusMode(pkg);
                if (!isReelApp(pkg)) {
                    reelCount = 0;
                    reelSessionStart = 0;
                } else if (reelSessionStart == 0) {
                    reelSessionStart = System.currentTimeMillis();
                }
            }
        }

        if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            String pkg = event.getPackageName() != null
                ? event.getPackageName().toString() : "";
            if (isReelApp(pkg)) {
                reelCount++;
                if (reelCount % 10 == 0) {
                    long sessionTime = System.currentTimeMillis() - reelSessionStart;
                    showReelPopup(reelCount, sessionTime);
                }
            }
        }
    }

    private boolean isReelApp(String pkg) {
        return AppConstants.INSTAGRAM_PKG.equals(pkg)
            || AppConstants.YOUTUBE_PKG.equals(pkg)
            || AppConstants.TIKTOK_PKG.equals(pkg);
    }

    private void checkIfLocked(String pkg) {
        if (pkg.isEmpty()) return;
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            AppUsageEntity e = db.appUsageDao()
                .getUsageForApp(pkg, TimeUtils.getTodayDate());
            if (e != null && e.isLocked) showLockerOverlay(e.appName);
        });
    }

    private void checkFocusMode(String pkg) {
        SharedPreferences prefs = getSharedPreferences(
            AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        boolean focusActive = prefs.getBoolean(AppConstants.PREF_FOCUS_ACTIVE, false);
        if (!focusActive) return;

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            AppUsageEntity e = db.appUsageDao()
                .getUsageForApp(pkg, TimeUtils.getTodayDate());
            if (e != null && !e.isFocusWhitelisted) {
                showFocusModeOverlay();
            }
        });
    }

    private void showLockerOverlay(String appName) {
        runOnUiThread(() -> {
            ensureWindowManager();
            removeLocker();
            LayoutInflater inf = LayoutInflater.from(this);
            lockerOverlay = inf.inflate(R.layout.overlay_app_locked, null);
            ((TextView) lockerOverlay.findViewById(R.id.tvLockedApp))
                .setText(appName + " is locked");
            ((TextView) lockerOverlay.findViewById(R.id.tvLockedReason))
                .setText("You set this limit — keep going.");
            lockerOverlay.findViewById(R.id.btnGoBack)
                .setOnClickListener(v -> { removeLocker(); performGlobalAction(GLOBAL_ACTION_BACK); });
            lockerOverlay.findViewById(R.id.btnUnlock)
                .setOnClickListener(v -> removeLocker());
            windowManager.addView(lockerOverlay, overlayParams(true));
        });
    }

    private void showFocusModeOverlay() {
        runOnUiThread(() -> {
            ensureWindowManager();
            removeLocker();
            LayoutInflater inf = LayoutInflater.from(this);
            lockerOverlay = inf.inflate(R.layout.overlay_app_locked, null);
            ((TextView) lockerOverlay.findViewById(R.id.tvLockedApp))
                .setText("Focus Mode is ON");
            ((TextView) lockerOverlay.findViewById(R.id.tvLockedReason))
                .setText("This app is not in your study whitelist.");
            lockerOverlay.findViewById(R.id.btnGoBack)
                .setOnClickListener(v -> { removeLocker(); performGlobalAction(GLOBAL_ACTION_BACK); });
            lockerOverlay.findViewById(R.id.btnUnlock)
                .setOnClickListener(v -> removeLocker());
            windowManager.addView(lockerOverlay, overlayParams(true));
        });
    }

    private void showReelPopup(int count, long sessionMillis) {
        runOnUiThread(() -> {
            ensureWindowManager();
            removeReelPopup();
            LayoutInflater inf = LayoutInflater.from(this);
            reelPopup = inf.inflate(R.layout.overlay_reel_popup, null);
            String mins = String.valueOf(sessionMillis / 60000);
            ((TextView) reelPopup.findViewById(R.id.tvReelCount))
                .setText(count + " reels watched");
            ((TextView) reelPopup.findViewById(R.id.tvReelTime))
                .setText(mins + " min on reels — still worth it?");
            reelPopup.findViewById(R.id.btnReelDismiss)
                .setOnClickListener(v -> removeReelPopup());
            WindowManager.LayoutParams p = overlayParams(false);
            p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            p.y = 80;
            windowManager.addView(reelPopup, p);
            reelPopup.postDelayed(this::removeReelPopup, 5000);
        });
    }

    private void runOnUiThread(Runnable r) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(r);
    }

    private void ensureWindowManager() {
        if (windowManager == null)
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private WindowManager.LayoutParams overlayParams(boolean fullScreen) {
        int w = fullScreen ? WindowManager.LayoutParams.MATCH_PARENT
                           : WindowManager.LayoutParams.WRAP_CONTENT;
        int h = fullScreen ? WindowManager.LayoutParams.MATCH_PARENT
                           : WindowManager.LayoutParams.WRAP_CONTENT;
        return new WindowManager.LayoutParams(w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    }

    private void removeLocker() {
        if (lockerOverlay != null && windowManager != null) {
            try { windowManager.removeView(lockerOverlay); } catch (Exception ignored) {}
            lockerOverlay = null;
        }
    }

    private void removeReelPopup() {
        if (reelPopup != null && windowManager != null) {
            try { windowManager.removeView(reelPopup); } catch (Exception ignored) {}
            reelPopup = null;
        }
    }

    @Override
    public void onInterrupt() {
        removeLocker();
        removeReelPopup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        removeLocker();
        removeReelPopup();
    }
}

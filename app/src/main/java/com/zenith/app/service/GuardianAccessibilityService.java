package com.zenith.app.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import com.zenith.app.R;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.db.entity.AppUsageEntity;
import com.zenith.app.db.entity.BrowserVisitEntity;
import com.zenith.app.util.AppConstants;
import com.zenith.app.util.TimeUtils;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GuardianAccessibilityService extends AccessibilityService {

    private WindowManager      windowManager;
    private View               lockerOverlay;
    private View               reelPopup;
    private View               browserBanner;

    private String  currentPkg       = "";
    private String  currentUrl       = "";
    private int     reelCount        = 0;
    private long    reelSessionStart = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler         uiHandler = new Handler(Looper.getMainLooper());

    // Known browser packages
    private static final List<String> BROWSER_PKGS = Arrays.asList(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "com.UCMobile.intl"
    );

    // URL bar node IDs for Chrome
    private static final List<String> URL_BAR_IDS = Arrays.asList(
        "com.android.chrome:id/url_bar",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.microsoft.emmx:id/url_bar",
        "com.brave.browser:id/url_bar"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int    type = event.getEventType();
        String pkg  = event.getPackageName() != null
            ? event.getPackageName().toString() : "";

        // ── App switch ──
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (!pkg.equals(currentPkg)) {
                currentPkg = pkg;
                checkIfLocked(pkg);
                checkFocusMode(pkg);
                if (!isReelApp(pkg)) { reelCount = 0; reelSessionStart = 0; }
                else if (reelSessionStart == 0) reelSessionStart = System.currentTimeMillis();
            }
        }

        // ── Browser URL reading ──
        if (BROWSER_PKGS.contains(pkg)) {
            String url = extractBrowserUrl(event, pkg);
            if (url != null && !url.isEmpty() && !url.equals(currentUrl)) {
                currentUrl = url;
                saveBrowserVisit(pkg, url);
                showBrowserBanner(pkg, url);
            }
        }

        // ── Reel counter ──
        if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && isReelApp(pkg)) {
            reelCount++;
            if (reelCount % 10 == 0) {
                long sessionMs = System.currentTimeMillis() - reelSessionStart;
                showReelPopup(reelCount, sessionMs);
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Browser URL extraction
    // ─────────────────────────────────────────────
    private String extractBrowserUrl(AccessibilityEvent event, String pkg) {
        // 1. Try getting text directly from event
        if (event.getText() != null && !event.getText().isEmpty()) {
            String txt = event.getText().get(0) != null
                ? event.getText().get(0).toString() : "";
            if (isLikelyUrl(txt)) return txt;
        }

        // 2. Traverse the window to find URL bar node
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;

        for (String id : URL_BAR_IDS) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                AccessibilityNodeInfo node = nodes.get(0);
                if (node.getText() != null) {
                    String url = node.getText().toString();
                    if (isLikelyUrl(url)) return url;
                }
            }
        }

        // 3. Recursively search for URL-like text
        return findUrlInTree(root);
    }

    private String findUrlInTree(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence txt = node.getText();
        if (txt != null && isLikelyUrl(txt.toString())) return txt.toString();
        for (int i = 0; i < node.getChildCount(); i++) {
            String found = findUrlInTree(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private boolean isLikelyUrl(String txt) {
        return txt != null && (txt.startsWith("http") || txt.startsWith("www.")
            || (txt.contains(".") && !txt.contains(" ") && txt.length() > 4));
    }

    // ─────────────────────────────────────────────
    //  Save browser visit to DB
    // ─────────────────────────────────────────────
    private void saveBrowserVisit(String pkg, String url) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            BrowserVisitEntity visit = new BrowserVisitEntity();
            visit.browserPackage = pkg;
            visit.url            = url;
            visit.visitedAt      = System.currentTimeMillis();
            visit.date           = TimeUtils.getTodayDate();
            db.browserVisitDao().insert(visit);
        });
    }

    // ─────────────────────────────────────────────
    //  Show browser banner overlay
    // ─────────────────────────────────────────────
    private void showBrowserBanner(String pkg, String url) {
        uiHandler.post(() -> {
            ensureWindowManager();
            removeBrowserBanner();
            LayoutInflater inf = LayoutInflater.from(this);
            browserBanner = inf.inflate(R.layout.overlay_browser_banner, null);

            String domain = extractDomain(url);
            ((TextView) browserBanner.findViewById(R.id.tvBrowserDomain)).setText(domain);
            ((TextView) browserBanner.findViewById(R.id.tvBrowserUrl)).setText(url);
            browserBanner.findViewById(R.id.btnBrowserClose)
                .setOnClickListener(v -> removeBrowserBanner());

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP;
            p.y = 80;
            windowManager.addView(browserBanner, p);

            // Auto-dismiss after 6 seconds
            uiHandler.postDelayed(this::removeBrowserBanner, 6000);
        });
    }

    private String extractDomain(String url) {
        try {
            String domain = url.replaceFirst("https?://", "").replaceFirst("www\\.", "");
            int slash = domain.indexOf('/');
            return slash > 0 ? domain.substring(0, slash) : domain;
        } catch (Exception e) { return url; }
    }

    // ─────────────────────────────────────────────
    //  App lock overlay
    // ─────────────────────────────────────────────
    private void checkIfLocked(String pkg) {
        if (pkg.isEmpty()) return;
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            AppUsageEntity e = db.appUsageDao()
                .getUsageForApp(pkg, TimeUtils.getTodayDate());
            if (e != null && e.isLocked) showLockerOverlay(e.appName, false);
        });
    }

    private void checkFocusMode(String pkg) {
        SharedPreferences prefs = getSharedPreferences(
            AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        boolean focusActive = prefs.getBoolean(AppConstants.PREF_FOCUS_ACTIVE, false);
        if (!focusActive) return;
        // Don't block Zenith itself
        if (pkg.equals(getPackageName())) return;

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            AppUsageEntity e = db.appUsageDao()
                .getUsageForApp(pkg, TimeUtils.getTodayDate());
            if (e != null && !e.isFocusWhitelisted) showLockerOverlay("Focus Mode", true);
        });
    }

    private void showLockerOverlay(String label, boolean isFocus) {
        uiHandler.post(() -> {
            ensureWindowManager();
            removeLocker();
            LayoutInflater inf = LayoutInflater.from(this);
            lockerOverlay = inf.inflate(R.layout.overlay_app_locked, null);

            ((TextView) lockerOverlay.findViewById(R.id.tvLockedApp))
                .setText(isFocus ? "🎯 Focus Mode Active" : "🔒 " + label + " is locked");
            ((TextView) lockerOverlay.findViewById(R.id.tvLockedReason))
                .setText(isFocus
                    ? "This app is not in your focus whitelist.\nStay on task!"
                    : "You hit your daily limit.\nIt unlocks tomorrow. Stay strong!");

            lockerOverlay.findViewById(R.id.btnGoBack).setOnClickListener(v -> {
                removeLocker();
                performGlobalAction(GLOBAL_ACTION_BACK);
            });
            lockerOverlay.findViewById(R.id.btnGoHome).setOnClickListener(v -> {
                removeLocker();
                performGlobalAction(GLOBAL_ACTION_HOME);
            });

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            windowManager.addView(lockerOverlay, p);
        });
    }

    // ─────────────────────────────────────────────
    //  Reel counter popup
    // ─────────────────────────────────────────────
    private void showReelPopup(int count, long sessionMs) {
        uiHandler.post(() -> {
            ensureWindowManager();
            removeReelPopup();
            LayoutInflater inf = LayoutInflater.from(this);
            reelPopup = inf.inflate(R.layout.overlay_reel_popup, null);
            String mins = String.valueOf(sessionMs / 60000);
            ((TextView) reelPopup.findViewById(R.id.tvReelCount))
                .setText(count + " reels watched");
            ((TextView) reelPopup.findViewById(R.id.tvReelTime))
                .setText(mins + " min on reels — still worth it?");
            reelPopup.findViewById(R.id.btnReelDismiss)
                .setOnClickListener(v -> removeReelPopup());
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            p.y = 80;
            windowManager.addView(reelPopup, p);
            uiHandler.postDelayed(this::removeReelPopup, 5000);
        });
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────
    private boolean isReelApp(String pkg) {
        return AppConstants.INSTAGRAM_PKG.equals(pkg)
            || AppConstants.YOUTUBE_PKG.equals(pkg)
            || AppConstants.TIKTOK_PKG.equals(pkg);
    }

    private void ensureWindowManager() {
        if (windowManager == null)
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
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

    private void removeBrowserBanner() {
        if (browserBanner != null && windowManager != null) {
            try { windowManager.removeView(browserBanner); } catch (Exception ignored) {}
            browserBanner = null;
        }
    }

    @Override public void onInterrupt() { removeLocker(); removeReelPopup(); removeBrowserBanner(); }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        uiHandler.removeCallbacksAndMessages(null);
        removeLocker(); removeReelPopup(); removeBrowserBanner();
    }
}

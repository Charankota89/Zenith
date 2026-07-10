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
    private View               limitWarningOverlay;
    private String             lastNotifiedApp  = "";
    private int                lastNotifiedPct  = 0;

    private String  currentPkg       = "";
    private String  currentUrl       = "";
    private int     reelCount        = 0;
    private long    reelSessionStart = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler         uiHandler = new Handler(Looper.getMainLooper());

    private long appOpenedTime = 0;

    private final Runnable limitCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndIncrementUsage();
            uiHandler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        appOpenedTime = System.currentTimeMillis();
        uiHandler.post(limitCheckerRunnable);
    }

    // Known browser packages
    private static final List<String> BROWSER_PKGS = Arrays.asList(
        "com.android.chrome",
        "org.mozilla.firefox",
        "org.mozilla.focus",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "com.UCMobile.intl",
        "com.duckduckgo.mobile.android"
    );

    // URL bar node IDs for major browsers
    private static final List<String> URL_BAR_IDS = Arrays.asList(
        "com.android.chrome:id/url_bar",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "org.mozilla.focus:id/urlView",
        "org.mozilla.focus:id/mozac_browser_toolbar_url_view",
        "com.microsoft.emmx:id/url_bar",
        "com.brave.browser:id/url_bar",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.sec.android.app.sbrowser:id/url_bar",
        "com.sec.android.app.sbrowser:id/url_text",
        "com.opera.browser:id/url_field",
        "com.opera.browser:id/url_bar",
        "com.UCMobile.intl:id/url_bar",
        "com.UCMobile.intl:id/search_btn",
        "com.duckduckgo.mobile.android:id/omnibarTextInput"
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
                checkAndIncrementUsage();
                currentPkg = pkg;
                appOpenedTime = System.currentTimeMillis();
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
        if (txt != null) {
            String val = txt.toString().trim();
            if (isLikelyUrl(val)) return val;
            
            // Fallback resource name checking for custom browser URL fields
            String resId = node.getViewIdResourceName();
            if (resId != null) {
                String idLower = resId.toLowerCase();
                if ((idLower.contains("url") || idLower.contains("location") || idLower.contains("address") || idLower.contains("search"))
                    && val.length() > 3 && val.contains(".") && !val.contains(" ")) {
                    return val;
                }
            }
        }
        
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

            View layoutDetails = lockerOverlay.findViewById(R.id.layoutRequestEmailDetails);
            View layoutEmailSent = lockerOverlay.findViewById(R.id.layoutEmailSent);
            if (isFocus) {
                layoutDetails.setVisibility(View.GONE);
                layoutEmailSent.setVisibility(View.GONE);
            } else {
                layoutDetails.setVisibility(View.VISIBLE);
                layoutEmailSent.setVisibility(View.GONE);
                
                SharedPreferences pr = getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
                String email = pr.getString("user_email", "ramcharan@gmail.com");

                lockerOverlay.findViewById(R.id.btnSendEmail).setOnClickListener(v -> {
                    android.widget.EditText etReason = lockerOverlay.findViewById(R.id.etUnlockReason);
                    String reasonText = etReason.getText().toString().trim();
                    if (reasonText.isEmpty()) {
                        // Shake feedback on empty input
                        android.view.animation.Animation shake = android.view.animation.AnimationUtils
                            .loadAnimation(this, R.anim.shake);
                        etReason.startAnimation(shake);
                        android.widget.Toast.makeText(this, "Please enter a reason to unlock the app.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    android.widget.RadioGroup rg = lockerOverlay.findViewById(R.id.rgDuration);
                    int checkedId = rg.getCheckedRadioButtonId();
                    long durationMin = 5;
                    if (checkedId == R.id.rb15) {
                        durationMin = 15;
                    } else if (checkedId == R.id.rb30) {
                        durationMin = 30;
                    }

                    android.widget.Toast.makeText(this, "Verification email sent to " + email, android.widget.Toast.LENGTH_LONG).show();
                    
                    // Post mock verification notification
                    postVerificationNotification(label, currentPkg, durationMin, reasonText);

                    ((TextView) lockerOverlay.findViewById(R.id.tvEmailSentMessage))
                        .setText("Verification link sent to " + email + "!\nPlease check your email (or tap the verification notification banner above) to verify and unlock.");
                    
                    // Crossfade animation between details inputs and email sent state
                    layoutDetails.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                        layoutDetails.setVisibility(View.GONE);
                        layoutEmailSent.setVisibility(View.VISIBLE);
                        layoutEmailSent.setAlpha(0f);
                        layoutEmailSent.animate().alpha(1f).setDuration(200).start();
                    }).start();
                });
            }

            lockerOverlay.findViewById(R.id.btnGoBack).setOnClickListener(v -> {
                removeLocker();
                performGlobalAction(GLOBAL_ACTION_BACK);
            });
            lockerOverlay.findViewById(R.id.btnGoHome).setOnClickListener(v -> {
                removeLocker();
                performGlobalAction(GLOBAL_ACTION_HOME);
            });

            // Entrance slide-up animation for dialog container
            View lockContainer = lockerOverlay.findViewById(R.id.layoutLockContainer);
            if (lockContainer != null) {
                android.view.animation.Animation anim = android.view.animation.AnimationUtils
                    .loadAnimation(this, R.anim.slide_up_fade_in);
                lockContainer.startAnimation(anim);
            }

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
    //  Real-time screen usage checking & warning
    // ─────────────────────────────────────────────
    private void checkAndIncrementUsage() {
        String pkg = currentPkg;
        if (pkg == null || pkg.isEmpty() || pkg.equals(getPackageName())) return;

        long now = System.currentTimeMillis();
        if (appOpenedTime == 0) appOpenedTime = now;
        long elapsed = now - appOpenedTime;
        appOpenedTime = now;

        if (elapsed <= 0) return;

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            String today = TimeUtils.getTodayDate();
            AppUsageEntity e = db.appUsageDao().getUsageForApp(pkg, today);
            if (e != null) {
                if (e.unlockExpiresAt > 0 && now < e.unlockExpiresAt) {
                    e.usageTimeMillis += elapsed;
                    db.appUsageDao().update(e);
                    uiHandler.post(this::removeLocker);
                    return;
                }
                
                if (e.unlockExpiresAt > 0 && now >= e.unlockExpiresAt) {
                    e.isLocked = true;
                    e.unlockExpiresAt = 0;
                    db.appUsageDao().update(e);
                    uiHandler.post(() -> showLockerOverlay(e.appName, false));
                    return;
                }

                e.usageTimeMillis += elapsed;
                if (e.limitMillis > 0) {
                    long pct = (e.usageTimeMillis * 100L) / e.limitMillis;
                    if (pct >= 100) {
                        e.isLocked = true;
                        db.appUsageDao().update(e);
                        uiHandler.post(() -> showLockerOverlay(e.appName, false));
                    } else if (pct >= 80) {
                        uiHandler.post(() -> showLimitWarningBanner(e.appName, 80, e.limitMillis - e.usageTimeMillis));
                    } else if (pct >= 50) {
                        uiHandler.post(() -> showLimitWarningBanner(e.appName, 50, e.limitMillis - e.usageTimeMillis));
                    }
                }
                db.appUsageDao().update(e);
            }
        });
    }

    private void showLimitWarningBanner(String appName, int pct, long remainingMs) {
        if (appName.equals(lastNotifiedApp) && pct == lastNotifiedPct) return;
        lastNotifiedApp = appName;
        lastNotifiedPct = pct;

        uiHandler.post(() -> {
            ensureWindowManager();
            if (limitWarningOverlay != null) {
                try { windowManager.removeView(limitWarningOverlay); } catch (Exception ignored) {}
            }

            LayoutInflater inf = LayoutInflater.from(this);
            limitWarningOverlay = inf.inflate(R.layout.overlay_browser_banner, null);

            TextView tvIcon = (TextView) ((android.view.ViewGroup) limitWarningOverlay).getChildAt(0);
            if (tvIcon != null) tvIcon.setText("⚠️");

            TextView tvTitle = limitWarningOverlay.findViewById(R.id.tvBrowserDomain);
            if (tvTitle != null) tvTitle.setText(appName + " limit warning");

            TextView tvDesc = limitWarningOverlay.findViewById(R.id.tvBrowserUrl);
            long remainingMins = remainingMs / 60000;
            if (tvDesc != null) {
                tvDesc.setText(pct + "% limit reached (" + remainingMins + " mins remaining)");
            }

            limitWarningOverlay.findViewById(R.id.btnBrowserClose).setOnClickListener(v -> {
                if (limitWarningOverlay != null && windowManager != null) {
                    try { windowManager.removeView(limitWarningOverlay); } catch (Exception ignored) {}
                    limitWarningOverlay = null;
                }
            });

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP;
            p.y = 80;
            windowManager.addView(limitWarningOverlay, p);

            uiHandler.postDelayed(() -> {
                if (limitWarningOverlay != null && windowManager != null) {
                    try { windowManager.removeView(limitWarningOverlay); } catch (Exception ignored) {}
                    limitWarningOverlay = null;
                }
            }, 6000);
        });
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────
    private void postVerificationNotification(String appName, String targetPkg, long durationMin, String reason) {
        String deepLink = "zenith://unlock?pkg=" + targetPkg + "&duration=" + durationMin + "&reason=" + android.net.Uri.encode(reason);
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(deepLink));
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
            this,
            999,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                "unlock_channel",
                "Zenith Unlocking",
                android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Email Verification Unlocks");
            nm.createNotificationChannel(channel);
        }

        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, "unlock_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("✉️ Verify Zenith Unlock request")
            .setContentText("Click here to approve unlock request for " + appName + " (" + durationMin + "m)")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build();

        nm.notify(999, notification);
    }
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
        uiHandler.removeCallbacks(limitCheckerRunnable);
        executor.shutdown();
        uiHandler.removeCallbacksAndMessages(null);
        removeLocker(); removeReelPopup(); removeBrowserBanner();
        if (limitWarningOverlay != null && windowManager != null) {
            try { windowManager.removeView(limitWarningOverlay); } catch (Exception ignored) {}
            limitWarningOverlay = null;
        }
    }
}

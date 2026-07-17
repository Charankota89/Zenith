package com.zenith.app.service;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ImageView;
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
    private View               lockerCapsule;
    private boolean            isCapsuleExpanded = false;
    private final Handler      capsuleHandler = new Handler(Looper.getMainLooper());
    private final Runnable     collapseRunnable = this::collapseCapsule;
    private String             lastToastAppPkg  = "";
    private long               currentAppUsedMs  = 0;
    private long               currentAppLimitMs = 0;
    private String             currentAppName    = "";
    private String             currentTasksStr   = "";
    private String             lastNotifiedApp  = "";
    private int                lastNotifiedPct  = 0;

    private String  currentPkg       = "";
    private String  currentUrl       = "";
    private int     reelCount        = 0;
    private long    reelSessionStart = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler         uiHandler = new Handler(Looper.getMainLooper());

    private long appOpenedTime = 0;

    // ── Screen on/off tracking ──────────────────────────────────────
    // Without this, usage time keeps accumulating while the phone is
    // locked in your pocket with an app still technically "foreground"
    // from Android's point of view. This was the main accuracy bug.
    private boolean screenIsOn = true;

    private final BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                    // Flush whatever time was genuinely accrued right up until
                    // the screen turned off, then pause the tracker entirely.
                    checkAndIncrementUsage();
                    screenIsOn = false;
                    uiHandler.removeCallbacks(limitCheckerRunnable);
                    uiHandler.post(GuardianAccessibilityService.this::removeFloatingCapsule);
                    break;
                case Intent.ACTION_SCREEN_ON:
                    // Resume tracking, but reset the checkpoint so the locked
                    // period itself is never counted as usage of whatever app
                    // happened to be in the foreground when the screen died.
                    screenIsOn = true;
                    appOpenedTime = System.currentTimeMillis();
                    uiHandler.removeCallbacks(limitCheckerRunnable);
                    uiHandler.post(limitCheckerRunnable);
                    break;
            }
        }
    };

    private final Runnable limitCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            if (screenIsOn) {
                checkAndIncrementUsage();
            }
            uiHandler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        appOpenedTime = System.currentTimeMillis();
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
        screenIsOn = pm == null || pm.isInteractive();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // API 33+ requires an explicit export flag for dynamically registered
        // receivers or this throws SecurityException at runtime. This is a
        // system-only protected broadcast, so NOT_EXPORTED is correct.
        androidx.core.content.ContextCompat.registerReceiver(
            this, screenStateReceiver, filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
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

    /**
     * True while the current browser tab is on a checkout/payment-gateway
     * page. Used to suppress the browser banner and skip logging the URL
     * to browser-visit history for privacy.
     */
    private boolean isSensitiveFinancialContext(String pkg) {
        if (AppConstants.FINANCIAL_APP_PACKAGES.contains(pkg)) return true;
        if (BROWSER_PKGS.contains(pkg) && currentUrl != null) {
            String lowerUrl = currentUrl.toLowerCase(java.util.Locale.ROOT);
            for (String keyword : AppConstants.PAYMENT_URL_KEYWORDS) {
                if (lowerUrl.contains(keyword)) return true;
            }
        }
        return false;
    }

    /** Immediately tears down every Zenith overlay — used the instant a
     *  financial context is detected, so nothing lingers on top of it. */
    private void suppressAllOverlaysForSensitiveContext() {
        uiHandler.post(() -> {
            removeFloatingCapsule();
            removeBrowserBanner();
            removeReelPopup();
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int    type = event.getEventType();
        String pkg  = event.getPackageName() != null
            ? event.getPackageName().toString() : "";

        // ── App switch ──
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (!pkg.equals(currentPkg)) {
                checkAndIncrementUsage(); // Save old app usage
                currentPkg = pkg;
                appOpenedTime = System.currentTimeMillis();

                if (AppConstants.FINANCIAL_APP_PACKAGES.contains(pkg)) {
                    // Entering a banking/payment app directly (not a browser
                    // checkout page) — tear down overlays immediately and
                    // skip the rest of this switch's bookkeeping.
                    suppressAllOverlaysForSensitiveContext();
                    return;
                }

                checkAndIncrementUsage(); // Trigger new app checks immediately
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
                if (isSensitiveFinancialContext(pkg)) {
                    // On a checkout/payment page: don't log it to browser
                    // history, and don't draw the banner on top of it.
                    suppressAllOverlaysForSensitiveContext();
                } else {
                    saveBrowserVisit(pkg, url);
                    showBrowserBanner(pkg, url);
                }
            }
        }

        // ── Reel counter ──
        if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && isReelApp(pkg)) {
            reelCount++;
            
            if (lockerCapsule != null && !currentAppName.isEmpty()) {
                if (!isCapsuleExpanded) {
                    updateCapsuleData(currentAppName, currentAppUsedMs, currentAppLimitMs);
                } else {
                    TextView tvDetail = lockerCapsule.findViewById(R.id.tvExpandedDetail);
                    if (tvDetail != null) {
                        String limitStr = TimeUtils.formatDuration(currentAppLimitMs);
                        String usedStr = TimeUtils.formatDuration(currentAppUsedMs);
                        String reelsStr = "\nReels Scrolled: " + reelCount;
                        tvDetail.setText("App: " + currentAppName + "\nLimit: " + limitStr + " • Used: " + usedStr + reelsStr + "\n" + currentTasksStr);
                    }
                }
            }
            
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
                String email = pr.getString("user_email", "");

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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            // Unlike the other overlays (capsule, banner), this one contains
            // a real text field for the unlock reason — it must be able to
            // receive focus or the keyboard can never appear. FLAG_NOT_FOCUSABLE
            // (used everywhere else in this file) was the entire bug here.
            p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
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
        if (pkg == null || pkg.isEmpty() || pkg.equals(getPackageName())) {
            lastToastAppPkg = "";
            uiHandler.post(this::removeFloatingCapsule);
            return;
        }
        if (AppConstants.FINANCIAL_APP_PACKAGES.contains(pkg)) {
            // Never track time, check limits, or draw any overlay while a
            // banking/payment app is open — still advance the checkpoint so
            // no backlog of time gets dumped onto whatever app opens next.
            appOpenedTime = System.currentTimeMillis();
            uiHandler.post(this::removeFloatingCapsule);
            return;
        }
        if (!screenIsOn) {
            // Defense-in-depth: even if some OEM fires a stray window event
            // while the screen is off, never count that time as usage.
            return;
        }

        long now = System.currentTimeMillis();
        if (appOpenedTime == 0) appOpenedTime = now;
        long elapsed = now - appOpenedTime;
        appOpenedTime = now;

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            String today = TimeUtils.getTodayDate();
            AppUsageEntity e = db.appUsageDao().getUsageForApp(pkg, today);
            
            if (e == null) {
                try {
                    android.content.pm.PackageManager pm = getPackageManager();
                    android.content.pm.ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    if ((info.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                        String appName = pm.getApplicationLabel(info).toString();
                        e = new AppUsageEntity();
                        e.packageName = pkg;
                        e.appName = appName;
                        e.usageTimeMillis = 0;
                        e.limitMillis = 0;
                        e.isLocked = false;
                        e.isFocusWhitelisted = true;
                        e.date = today;
                        db.appUsageDao().insert(e);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (e != null) {
                final AppUsageEntity finalEntity = e;
                currentAppName = finalEntity.appName;

                // Once an app is locked, the locker overlay sits on top of it but
                // the underlying app is still technically "foreground" from
                // Android's point of view — without this guard, usage time would
                // keep climbing forever every 5 seconds even though the user can't
                // actually interact with the app anymore.
                if (finalEntity.isLocked) {
                    uiHandler.post(() -> {
                        removeFloatingCapsule();
                        showLockerOverlay(finalEntity.appName, false);
                    });
                    return;
                }

                if (elapsed > 0) {
                    finalEntity.usageTimeMillis += elapsed;
                }
                currentAppUsedMs = finalEntity.usageTimeMillis;
                currentAppLimitMs = finalEntity.limitMillis;

                if (finalEntity.unlockExpiresAt > 0 && now < finalEntity.unlockExpiresAt) {
                    if (elapsed > 0) db.appUsageDao().update(finalEntity);
                    uiHandler.post(() -> {
                        removeLocker();
                        removeFloatingCapsule();
                    });
                    return;
                }
                
                if (finalEntity.unlockExpiresAt > 0 && now >= finalEntity.unlockExpiresAt) {
                    finalEntity.isLocked = true;
                    finalEntity.unlockExpiresAt = 0;
                    db.appUsageDao().update(finalEntity);
                    uiHandler.post(() -> {
                        removeFloatingCapsule();
                        showLockerOverlay(finalEntity.appName, false);
                    });
                    return;
                }

                if (finalEntity.limitMillis > 0) {
                    if (!pkg.equals(lastToastAppPkg)) {
                        lastToastAppPkg = pkg;
                        uiHandler.post(() -> {
                            android.widget.Toast.makeText(getApplicationContext(),
                                "You are under monitoring with Zenith",
                                android.widget.Toast.LENGTH_SHORT).show();
                        });
                    }

                    long pct = (finalEntity.usageTimeMillis * 100L) / finalEntity.limitMillis;
                    if (pct >= 100) {
                        finalEntity.isLocked = true;
                        db.appUsageDao().update(finalEntity);
                        uiHandler.post(() -> {
                            removeFloatingCapsule();
                            showLockerOverlay(finalEntity.appName, false);
                        });
                    } else {
                        final long usedVal = finalEntity.usageTimeMillis;
                        final long limitVal = finalEntity.limitMillis;
                        final String nameVal = finalEntity.appName;
                        uiHandler.post(() -> showFloatingCapsule(nameVal, usedVal, limitVal));

                        if (pct >= 80) {
                            uiHandler.post(() -> showLimitWarningBanner(finalEntity.appName, 80, finalEntity.limitMillis - finalEntity.usageTimeMillis));
                        } else if (pct >= 50) {
                            uiHandler.post(() -> showLimitWarningBanner(finalEntity.appName, 50, finalEntity.limitMillis - finalEntity.usageTimeMillis));
                        }
                    }
                } else {
                    uiHandler.post(this::removeFloatingCapsule);
                }
                if (elapsed > 0) {
                    db.appUsageDao().update(finalEntity);
                }
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

    @Override 
    public void onInterrupt() { 
        removeLocker(); 
        removeReelPopup(); 
        removeBrowserBanner(); 
        removeFloatingCapsule();
    }

    private void showFloatingCapsule(String appName, long usedMs, long limitMs) {
        uiHandler.post(() -> {
            ensureWindowManager();
            if (lockerCapsule == null) {
                LayoutInflater inf = LayoutInflater.from(this);
                lockerCapsule = inf.inflate(R.layout.overlay_floating_capsule, null);

                WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                    PixelFormat.TRANSLUCENT);
                p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                p.y = 40; // Float right below status bar / notch

                // Bounce scale entering transition
                lockerCapsule.setScaleX(0f);
                lockerCapsule.setScaleY(0f);
                lockerCapsule.animate().scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator()).start();

                windowManager.addView(lockerCapsule, p);

                View container = lockerCapsule.findViewById(R.id.capsule_container);
                View expandedLayout = lockerCapsule.findViewById(R.id.layoutExpandedDetails);
                container.setOnClickListener(v -> toggleCapsule(container, expandedLayout, appName, usedMs, limitMs));

                // Load the real app icon on a background thread, then apply it
                ImageView ivIcon = lockerCapsule.findViewById(R.id.ivCapsuleAppIcon);
                if (ivIcon != null) {
                    final String pkgForIcon = currentPkg;
                    executor.execute(() -> {
                        try {
                            android.graphics.drawable.Drawable icon =
                                getPackageManager().getApplicationIcon(pkgForIcon);
                            uiHandler.post(() -> {
                                if (lockerCapsule != null) ivIcon.setImageDrawable(icon);
                            });
                        } catch (Exception ignored) {}
                    });
                }
            }

            if (!isCapsuleExpanded) {
                updateCapsuleData(appName, usedMs, limitMs);
            }
        });
    }

    private void updateCapsuleData(String appName, long usedMs, long limitMs) {
        if (lockerCapsule == null) return;
 
        long remainingMs = limitMs - usedMs;
        long remainingMins = Math.max(0, remainingMs / 60000);
 
        TextView tvText = lockerCapsule.findViewById(R.id.tvCapsuleText);
        View dot = lockerCapsule.findViewById(R.id.viewStatusDot);
        android.widget.ProgressBar progressBar = lockerCapsule.findViewById(R.id.progressCapsuleUsage);
        View container = lockerCapsule.findViewById(R.id.capsule_container);
 
        if (tvText != null) {
            String reelsStr = "";
            if (isReelApp(currentPkg) && reelCount > 0) {
                reelsStr = " • " + reelCount + " reels";
            }
            tvText.setText(appName + ": " + remainingMins + "m left" + reelsStr);
        }

        long pct = limitMs > 0 ? (usedMs * 100L) / limitMs : 0;

        if (progressBar != null) {
            progressBar.setProgress((int) Math.min(100, pct));
        }
 
        if (dot != null) {
            String colorHex = "#34D399"; // Green
            if (pct >= 80) {
                colorHex = "#F87171"; // Red
            } else if (pct >= 50) {
                colorHex = "#FBBF24"; // Amber
            }
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorHex)));
        }

        // Urgency pulse: once usage crosses 80%, gently pulse the whole capsule
        // to draw the eye, without being obnoxious about it. Stops automatically
        // once usage drops back below the threshold (e.g. after a reset).
        if (container != null) {
            boolean shouldPulse = pct >= 80;
            boolean isPulsing = container.getTag(R.id.tag_is_pulsing) != null
                && (boolean) container.getTag(R.id.tag_is_pulsing);
            if (shouldPulse && !isPulsing) {
                startCapsulePulse(container);
            } else if (!shouldPulse && isPulsing) {
                stopCapsulePulse(container);
            }
        }
    }

    private void startCapsulePulse(View container) {
        container.setTag(R.id.tag_is_pulsing, true);
        android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator.ofFloat(
            container, "alpha", 1f, 0.65f, 1f);
        pulse.setDuration(1100);
        pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        container.setTag(R.id.tag_pulse_animator_ref, pulse);
        pulse.start();
    }

    private void stopCapsulePulse(View container) {
        container.setTag(R.id.tag_is_pulsing, false);
        Object animObj = container.getTag(R.id.tag_pulse_animator_ref);
        if (animObj instanceof android.animation.ObjectAnimator) {
            ((android.animation.ObjectAnimator) animObj).cancel();
        }
        container.setAlpha(1f);
    }

    private void toggleCapsule(View container, View expandedLayout, String appName, long usedMs, long limitMs) {
        capsuleHandler.removeCallbacks(collapseRunnable);
        if (isCapsuleExpanded) {
            collapseCapsule();
        } else {
            expandCapsule(container, expandedLayout, appName, usedMs, limitMs);
            capsuleHandler.postDelayed(collapseRunnable, 4000); // Collapse after 4s
        }
    }

    private void expandCapsule(View container, View expandedLayout, String appName, long usedMs, long limitMs) {
        if (isCapsuleExpanded) return;
        isCapsuleExpanded = true;

        TextView tvDetail = container.findViewById(R.id.tvExpandedDetail);
        if (tvDetail != null) {
            String limitStr = TimeUtils.formatDuration(limitMs);
            String usedStr = TimeUtils.formatDuration(usedMs);
            tvDetail.setText("App: " + appName + "\nLimit: " + limitStr + " • Used: " + usedStr + "\nLoading tasks...");

            executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                java.util.List<com.zenith.app.db.entity.HabitEntity> habits = db.habitDao().getAllHabitsSync();

                StringBuilder tasksBuilder = new StringBuilder();
                int uncompletedCount = 0;
                for (com.zenith.app.db.entity.HabitEntity habit : habits) {
                    if (!habit.completedToday) {
                        uncompletedCount++;
                        if (tasksBuilder.length() > 0) {
                            tasksBuilder.append(", ");
                        }
                        tasksBuilder.append(habit.habitName);
                    }
                }

                final String tasksStr;
                if (habits.isEmpty()) {
                    tasksStr = "No tasks set in Career tab.";
                } else if (uncompletedCount == 0) {
                    tasksStr = "All tasks completed! 🎉";
                } else {
                    tasksStr = "Tasks left: " + tasksBuilder.toString();
                }

                uiHandler.post(() -> {
                    if (lockerCapsule != null && isCapsuleExpanded) {
                        tvDetail.setText("App: " + appName + "\nLimit: " + limitStr + " • Used: " + usedStr + "\n" + tasksStr);
                    }
                });
            });
        }

        if (expandedLayout != null) {
            expandedLayout.setVisibility(View.VISIBLE);
            expandedLayout.setAlpha(0f);
            expandedLayout.animate().alpha(1f).setDuration(200).start();
        }

        animateViewHeightAndWidth(container, dpToPx(160), dpToPx(240), dpToPx(36), dpToPx(96));
    }

    private void collapseCapsule() {
        if (!isCapsuleExpanded || lockerCapsule == null) return;
        isCapsuleExpanded = false;

        View container = lockerCapsule.findViewById(R.id.capsule_container);
        View expandedLayout = lockerCapsule.findViewById(R.id.layoutExpandedDetails);

        if (expandedLayout != null) {
            expandedLayout.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                expandedLayout.setVisibility(View.GONE);
            }).start();
        }

        if (container != null) {
            animateViewHeightAndWidth(container, dpToPx(240), dpToPx(160), dpToPx(96), dpToPx(36));
        }
    }

    private void animateViewHeightAndWidth(View view, int startWidth, int endWidth, int startHeight, int endHeight) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                lp.width = (int) (startWidth + (endWidth - startWidth) * fraction);
                lp.height = (int) (startHeight + (endHeight - startHeight) * fraction);
                view.setLayoutParams(lp);
                if (lockerCapsule != null && windowManager != null) {
                    try {
                        windowManager.updateViewLayout(lockerCapsule, lockerCapsule.getLayoutParams());
                    } catch (Exception ignored) {}
                }
            }
        });
        animator.setDuration(250);
        animator.start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void removeFloatingCapsule() {
        uiHandler.post(() -> {
            if (lockerCapsule != null && windowManager != null) {
                final View capsuleToRemove = lockerCapsule;
                View container = capsuleToRemove.findViewById(R.id.capsule_container);
                if (container != null) {
                    stopCapsulePulse(container);
                }
                lockerCapsule = null; // clear reference immediately so re-entrant calls don't double-animate
                isCapsuleExpanded = false;

                capsuleToRemove.animate()
                    .scaleX(0f).scaleY(0f).alpha(0f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        try {
                            windowManager.removeView(capsuleToRemove);
                        } catch (Exception ignored) {}
                    })
                    .start();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(screenStateReceiver); } catch (Exception ignored) {}
        uiHandler.removeCallbacks(limitCheckerRunnable);
        capsuleHandler.removeCallbacks(collapseRunnable);
        executor.shutdown();
        uiHandler.removeCallbacksAndMessages(null);
        capsuleHandler.removeCallbacksAndMessages(null);
        removeLocker(); 
        removeReelPopup(); 
        removeBrowserBanner();
        removeFloatingCapsule();
        if (limitWarningOverlay != null && windowManager != null) {
            try { windowManager.removeView(limitWarningOverlay); } catch (Exception ignored) {}
            limitWarningOverlay = null;
        }
    }
}

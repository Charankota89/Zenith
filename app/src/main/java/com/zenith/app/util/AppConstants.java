package com.zenith.app.util;

import java.util.Arrays;

public class AppConstants {
    public static final String CHANNEL_ID_USAGE     = "usage_alerts";
    public static final String CHANNEL_ID_FOCUS     = "focus_mode";
    public static final String CHANNEL_ID_WELLBEING = "wellbeing";
    public static final String CHANNEL_ID_CAREER    = "career";

    public static final String PREF_NAME            = "zenith_prefs";
    public static final String PREF_PIN             = "user_pin";
    public static final String PREF_FOCUS_ACTIVE    = "focus_mode_active";
    public static final String PREF_CAREER_ACTIVE   = "career_mode_active";
    public static final String PREF_BEDTIME_HOUR    = "bedtime_hour";
    public static final String PREF_BEDTIME_MINUTE  = "bedtime_minute";
    public static final String PREF_ONBOARDED       = "onboarded";
    public static final String PREF_DAILY_GOAL_MIN  = "daily_goal_minutes";
    public static final String PREF_POMODORO_ACTIVE = "pomodoro_active";

    public static final int NOTIF_ID_USAGE_MONITOR = 1001;
    public static final int NOTIF_ID_FOCUS         = 1002;
    public static final int NOTIF_ID_EYE_BREAK     = 1003;
    public static final int NOTIF_ID_DAILY_SUMMARY = 1004;
    public static final int NOTIF_ID_POSTURE       = 1005;
    public static final int NOTIF_ID_MOOD          = 1006;
    public static final int NOTIF_ID_CAREER        = 1007;
    public static final int NOTIF_ID_POMODORO      = 1008;

    public static final String INSTAGRAM_PKG = "com.instagram.android";
    public static final String YOUTUBE_PKG   = "com.google.android.youtube";
    public static final String TIKTOK_PKG    = "com.zhiliaoapp.musically";

    // ── Financial / payment apps ──────────────────────────────────
    // Zenith suppresses all of its own overlays (Dynamic Island, browser
    // banner, lock screen) while any of these are in the foreground, since
    // (a) drawing overlays on top of a payment screen risks blocking a real
    // button tap during a transaction, and (b) many banking apps actively
    // detect running Accessibility Services as a security risk and refuse
    // to display sensitive screens, or warn the user, while one is active.
    public static final java.util.Set<String> FINANCIAL_APP_PACKAGES = new java.util.HashSet<>(Arrays.asList(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                      // BHIM
        "com.dreamplug.androidapp",                // CRED
        "com.freecharge.android",
        "com.mobikwik_new",
        "com.paypal.android.p2pmobile",
        "com.sbi.SBIFreedomPlus",                  // SBI YONO/Freedom
        "com.sbi.yono",
        "com.snapwork.hdfc",                       // HDFC Bank
        "com.csam.icici.bank.imobile",             // ICICI iMobile
        "com.icicibank.pockets",
        "com.axis.mobile",                         // Axis Bank
        "com.msf.kbank.mobile",                    // Kotak Bank
        "com.axio.axiomobile",
        "com.idbibank.mpassbook",
        "com.infrasoft.uboi",
        "com.enstage.wibmo.hdfc",
        "com.bankofbaroda.upi",
        "com.pnb.pnbone"
    ));

    // URL substrings that indicate a checkout / payment-gateway page inside
    // a browser. Matching is a simple case-insensitive "contains" check.
    public static final java.util.Set<String> PAYMENT_URL_KEYWORDS = new java.util.HashSet<>(Arrays.asList(
        "checkout", "payment", "razorpay", "payu", "billdesk", "ccavenue",
        "cashfree", "instamojo", "paypal.com", "stripe.com", "upi://",
        "netbanking", "cardpayment", "pay.google.com", "paytm.com/pay",
        "phonepe.com/pay"
    ));

    public static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE";
    public static final String GEMINI_URL     =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
}

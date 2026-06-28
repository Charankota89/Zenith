package com.zenith.app.utils;

/**
 * Application-wide constants for all modules.
 */
public final class Constants {

    private Constants() {}

    // ─── SharedPreferences keys ──────────────────────────
    public static final String PREFS_NAME              = "zenith_prefs";
    public static final String PREF_PIN                = "lock_pin";
    public static final String PREF_BIOMETRIC_ENABLED  = "biometric_enabled";
    public static final String PREF_FOCUS_MODE_ACTIVE  = "focus_mode_active";
    public static final String PREF_CAREER_MODE_ACTIVE = "career_mode_active";
    public static final String PREF_SLEEP_TIME         = "sleep_time_hhmm";       // e.g. "22:30"
    public static final String PREF_STUDY_GOAL_MINUTES = "study_goal_minutes";
    public static final String PREF_TOTAL_XP           = "total_xp";
    public static final String PREF_USER_LEVEL         = "user_level";
    public static final String PREF_LAST_MOOD_DATE     = "last_mood_check_date";
    public static final String PREF_ONBOARDING_DONE    = "onboarding_done";
    public static final String PREF_GEMINI_API_KEY     = "gemini_api_key";

    // ─── Notification IDs ────────────────────────────────
    public static final int NOTIF_FOREGROUND_SERVICE   = 1001;
    public static final int NOTIF_USAGE_ALERT          = 1002;
    public static final int NOTIF_DAILY_SUMMARY        = 1003;
    public static final int NOTIF_POMODORO             = 1004;
    public static final int NOTIF_GOAL_MET             = 1005;
    public static final int NOTIF_EYE_BREAK            = 1006;
    public static final int NOTIF_POSTURE              = 1007;
    public static final int NOTIF_MOOD_CHECKIN         = 1008;
    public static final int NOTIF_REEL_POPUP           = 1009;
    public static final int NOTIF_BEDTIME              = 1010;

    // ─── Notification Channels ───────────────────────────
    public static final String CHANNEL_USAGE_ALERTS    = "channel_usage_alerts";
    public static final String CHANNEL_FOCUS           = "channel_focus";
    public static final String CHANNEL_WELLNESS        = "channel_wellness";
    public static final String CHANNEL_FOREGROUND      = "channel_foreground";

    // ─── Whitelist types ─────────────────────────────────
    public static final String WHITELIST_STUDY         = "study";
    public static final String WHITELIST_CAREER        = "career";

    // ─── Reel counter trigger threshold ─────────────────
    public static final int REEL_POPUP_EVERY           = 10;

    // ─── XP values ───────────────────────────────────────
    public static final int XP_GOAL_MET                = 50;
    public static final int XP_HABIT_COMPLETE          = 20;
    public static final int XP_FOCUS_SESSION           = 30;
    public static final int XP_WEEKLY_REPORT           = 100;

    // ─── Level thresholds ────────────────────────────────
    public static final int XP_LEVEL_FOCUSED           = 200;
    public static final int XP_LEVEL_DISCIPLINED       = 600;
    public static final int XP_LEVEL_CHAMPION          = 1500;

    // ─── Pomodoro defaults ───────────────────────────────
    public static final int POMODORO_WORK_MINUTES      = 25;
    public static final int POMODORO_BREAK_MINUTES     = 5;

    // ─── Eye break interval ──────────────────────────────
    public static final int EYE_BREAK_INTERVAL_MINUTES = 20;
    public static final int POSTURE_ALERT_INTERVAL_MINUTES = 45;

    // ─── Usage sync interval (WorkManager periodic) ──────
    public static final int USAGE_SYNC_INTERVAL_MINUTES = 15;

    // ─── Reel tracking platforms ─────────────────────────
    public static final String REEL_PLATFORM_INSTAGRAM = "Instagram";
    public static final String REEL_PLATFORM_YOUTUBE   = "YouTube";
    public static final String REEL_PLATFORM_TIKTOK    = "TikTok";

    public static final String PKG_INSTAGRAM           = "com.instagram.android";
    public static final String PKG_YOUTUBE             = "com.google.android.youtube";
    public static final String PKG_TIKTOK              = "com.zhiliaoapp.musically";

    // ─── Work tags ───────────────────────────────────────
    public static final String WORK_USAGE_SYNC         = "work_usage_sync";
    public static final String WORK_MIDNIGHT_RESET     = "work_midnight_reset";
    public static final String WORK_DAILY_SUMMARY      = "work_daily_summary";
    public static final String WORK_EYE_BREAK          = "work_eye_break";
    public static final String WORK_POSTURE            = "work_posture";

    // ─── Intent extras ───────────────────────────────────
    public static final String EXTRA_PACKAGE_NAME      = "extra_package_name";
    public static final String EXTRA_APP_NAME          = "extra_app_name";
    public static final String EXTRA_FROM_FRAGMENT     = "extra_from_fragment";
}

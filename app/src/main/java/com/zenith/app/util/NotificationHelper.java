package com.zenith.app.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import com.zenith.app.R;
import com.zenith.app.ui.MainActivity;

/**
 * Centralized notification helper.
 * All app notifications go through here to ensure consistent styling,
 * correct channels, and deep-link tap targets.
 */
public class NotificationHelper {

    // ── Deep-link tab IDs (match bottom_nav_menu item IDs) ──────────────────
    public static final String EXTRA_TARGET_TAB = "target_tab";
    public static final String TAB_HOME         = "nav_home";
    public static final String TAB_SCREEN       = "nav_screen";
    public static final String TAB_FOCUS        = "nav_focus";
    public static final String TAB_WELLBEING    = "nav_wellbeing";
    public static final String TAB_CAREER       = "nav_career";

    // ── Pomodoro timer notifications ─────────────────────────────────────────
    public static void notifyPomodoroSessionDone(Context ctx, int sessionsToday) {
        send(ctx,
            AppConstants.CHANNEL_ID_FOCUS,
            AppConstants.NOTIF_ID_POMODORO,
            "🎯 Focus Session Complete!",
            sessionsToday + " session" + (sessionsToday == 1 ? "" : "s") +
                " done today. Take your 5-minute break.",
            TAB_FOCUS,
            android.R.drawable.ic_menu_today,
            NotificationCompat.PRIORITY_HIGH);
    }

    public static void notifyPomodoroBreakDone(Context ctx) {
        send(ctx,
            AppConstants.CHANNEL_ID_FOCUS,
            AppConstants.NOTIF_ID_POMODORO + 1,
            "⏰ Break Over — Let's Go!",
            "Your break is done. Time to lock in for another 25 minutes.",
            TAB_FOCUS,
            android.R.drawable.ic_menu_today,
            NotificationCompat.PRIORITY_HIGH);
    }

    // ── Eye break (20-20-20 rule) ─────────────────────────────────────────────
    public static void notifyEyeBreak(Context ctx) {
        send(ctx,
            AppConstants.CHANNEL_ID_WELLBEING,
            AppConstants.NOTIF_ID_EYE_BREAK,
            "👁️ 20-20-20 Eye Break",
            "Look 20 feet away for 20 seconds. Your eyes deserve it.",
            TAB_WELLBEING,
            android.R.drawable.ic_menu_view,
            NotificationCompat.PRIORITY_DEFAULT);
    }

    // ── Posture check ─────────────────────────────────────────────────────────
    public static void notifyPostureCheck(Context ctx) {
        send(ctx,
            AppConstants.CHANNEL_ID_WELLBEING,
            AppConstants.NOTIF_ID_POSTURE,
            "🧍 Posture Check",
            "Sit straight, roll your shoulders. 30 seconds is all it takes.",
            TAB_WELLBEING,
            android.R.drawable.ic_menu_compass,
            NotificationCompat.PRIORITY_DEFAULT);
    }

    // ── Mood check-in reminder ────────────────────────────────────────────────
    public static void notifyMoodCheckIn(Context ctx) {
        send(ctx,
            AppConstants.CHANNEL_ID_WELLBEING,
            AppConstants.NOTIF_ID_MOOD,
            "😊 How are you feeling?",
            "Take 5 seconds to log your mood. Small reflections, big clarity.",
            TAB_WELLBEING,
            android.R.drawable.ic_dialog_info,
            NotificationCompat.PRIORITY_LOW);
    }

    // ── App time limit warnings ───────────────────────────────────────────────
    public static void notifyLimitWarning(Context ctx, String appName, int pct) {
        String title = appName + " — " + pct + "% of daily limit";
        String msg   = pct >= 100
            ? "Limit reached. " + appName + " is locked for today. Stay on track!"
            : "You're at " + pct + "% of your limit for " + appName + ". Wrap up soon.";
        send(ctx,
            AppConstants.CHANNEL_ID_USAGE,
            AppConstants.NOTIF_ID_USAGE_MONITOR + appName.hashCode() % 100,
            title, msg,
            TAB_SCREEN,
            android.R.drawable.ic_dialog_alert,
            NotificationCompat.PRIORITY_HIGH);
    }

    // ── Habit streak celebration ──────────────────────────────────────────────
    public static void notifyHabitStreak(Context ctx, String habitName, int streak) {
        send(ctx,
            AppConstants.CHANNEL_ID_CAREER,
            AppConstants.NOTIF_ID_CAREER,
            "🔥 " + streak + "-Day Streak! Keep going!",
            habitName + " — " + streak + " days in a row. Don't break the chain.",
            TAB_CAREER,
            android.R.drawable.ic_menu_agenda,
            NotificationCompat.PRIORITY_DEFAULT);
    }

    // ── Daily summary ─────────────────────────────────────────────────────────
    public static void notifyDailySummary(Context ctx,
                                           long totalScreenMs,
                                           int habitsCompleted,
                                           int pomodoroSessions) {
        long totalMins  = totalScreenMs / 60000;
        String title    = "📊 Today's Zenith Summary";
        String msg      = "📱 " + totalMins + "m screen time  •  " +
                          "✅ " + habitsCompleted + " habits  •  " +
                          "🎯 " + pomodoroSessions + " focus sessions";
        send(ctx,
            AppConstants.CHANNEL_ID_USAGE,
            AppConstants.NOTIF_ID_DAILY_SUMMARY,
            title, msg,
            TAB_HOME,
            android.R.drawable.ic_menu_info_details,
            NotificationCompat.PRIORITY_DEFAULT);
    }

    // ── Internal sender ──────────────────────────────────────────────────────
    private static void send(Context ctx, String channelId, int notifId,
                              String title, String msg, String tabTarget,
                              int iconRes, int priority) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_TARGET_TAB, tabTarget);

        PendingIntent pi = PendingIntent.getActivity(ctx, notifId, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(msg)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(new long[]{0, 150, 80, 150});

        NotificationManager nm = (NotificationManager)
            ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notifId, builder.build());
    }
}

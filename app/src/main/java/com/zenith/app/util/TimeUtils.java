package com.zenith.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public static String formatDuration(long millis) {
        long hours   = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        if (hours > 0)   return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        if (minutes > 0) return String.format(Locale.getDefault(), "%dm %ds", minutes, seconds);
        return String.format(Locale.getDefault(), "%ds", seconds);
    }

    public static long millisToMinutes(long millis) {
        return TimeUnit.MILLISECONDS.toMinutes(millis);
    }

    public static String getDayOfWeek() {
        return new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
    }
}

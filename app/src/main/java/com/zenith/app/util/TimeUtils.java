package com.zenith.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /** Returns a time-of-day-appropriate greeting for the current local hour. */
    public static String getGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5  && hour < 12) return "Good morning";
        if (hour >= 12 && hour < 17) return "Good afternoon";
        if (hour >= 17 && hour < 21) return "Good evening";
        return "Good night";
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

    /** Returns yyyy-MM-dd for N days before today (0 = today). */
    public static String getDateDaysAgo(int daysAgo) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    /** Short day label ("Mon", "Tue"...) for a yyyy-MM-dd date string. */
    public static String getShortDayLabel(String yyyyMMdd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(yyyyMMdd);
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return "";
        }
    }
}

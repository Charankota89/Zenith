package com.zenith.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Shared date/time utility methods used across all modules.
 */
public final class DateUtils {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DateUtils() {}

    /** Returns today's date as yyyy-MM-dd string. */
    public static String today() {
        return DATE_FORMAT.format(new Date());
    }

    /** Returns the date N days ago as yyyy-MM-dd string. */
    public static String daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return DATE_FORMAT.format(cal.getTime());
    }

    /** Returns epoch millis for the start of today (midnight). */
    public static long startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** Returns epoch millis for the start of the current week (Monday). */
    public static long startOfThisWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** Formats a duration in minutes to "Xh Ym" string. */
    public static String formatMinutes(long minutes) {
        if (minutes < 60) {
            return minutes + "m";
        }
        long h = minutes / 60;
        long m = minutes % 60;
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }

    /** Formats epoch millis to HH:mm string. */
    public static String formatTime(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        return sdf.format(new Date(millis));
    }

    /** Formats a yyyy-MM-dd string to a friendly label e.g. "Mon, Jun 28". */
    public static String friendlyDate(String dateStr) {
        try {
            Date d = DATE_FORMAT.parse(dateStr);
            SimpleDateFormat friendly = new SimpleDateFormat("EEE, MMM d", Locale.US);
            return friendly.format(d);
        } catch (Exception e) {
            return dateStr;
        }
    }

    /** Returns the day-of-week short label for a date string e.g. "Mon". */
    public static String dayLabel(String dateStr) {
        try {
            Date d = DATE_FORMAT.parse(dateStr);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.US);
            return sdf.format(d);
        } catch (Exception e) {
            return dateStr;
        }
    }
}

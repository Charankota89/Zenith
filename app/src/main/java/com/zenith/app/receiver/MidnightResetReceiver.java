package com.zenith.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.zenith.app.service.MidnightResetWorker;
import com.zenith.app.util.TimeUtils;

/**
 * Listens for {@link Intent#ACTION_DATE_CHANGED} — the system broadcast that
 * fires exactly at midnight when Android rolls the calendar date forward.
 *
 * This is a belt-and-suspenders fallback to guarantee the midnight reset runs
 * even if WorkManager's scheduled job is delayed (e.g. Doze mode, battery
 * optimisation, or OEM background-kill) beyond the actual midnight boundary.
 *
 * Note: ACTION_DATE_CHANGED cannot be declared in the manifest; it MUST be
 * registered dynamically at runtime. Registration is done in
 * {@link com.zenith.app.service.UsageMonitorService} because that service
 * runs persistently as a foreground service. This receiver class only
 * contains the action — all lifecycle management lives in the service.
 */
public class MidnightResetReceiver extends BroadcastReceiver {

    private static String lastResetDate = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String today = TimeUtils.getTodayDate();

        // Guard against duplicate fires within the same calendar day.
        // Android can fire ACTION_DATE_CHANGED more than once if the user
        // adjusts the clock, and ACTION_TIME_CHANGED fires on every NTP sync.
        // Only run the reset once per calendar date.
        if (today.equals(lastResetDate)) return;
        lastResetDate = today;

        // Enqueue an immediate (no delay) run of MidnightResetWorker. Using
        // OneTimeWorkRequest here (not PeriodicWorkRequest) guarantees the work
        // executes right now rather than at the next periodic window.
        OneTimeWorkRequest immediate = new OneTimeWorkRequest.Builder(MidnightResetWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(immediate);
    }
}

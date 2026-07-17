package com.zenith.app.util;

import android.content.Context;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.zenith.app.service.MidnightResetWorker;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Schedules {@link MidnightResetWorker} to run once, at the next real
 * calendar midnight (device-local time), using a plain OneTimeWorkRequest
 * rather than a fixed-interval PeriodicWorkRequest.
 *
 * Why: WorkManager's 24-hour PeriodicWorkRequest reschedules the next run
 * 24 hours after whenever the PREVIOUS run actually executed — not 24
 * hours after true midnight. Doze mode, battery optimization, and OEM
 * background restrictions routinely delay background jobs by anywhere
 * from minutes to hours, and each delay compounds: after a few days of
 * drift, "midnight reset" stops happening anywhere near midnight and
 * instead free-floats as a rolling ~24-hour window, which is exactly the
 * bug this class fixes. The worker calls scheduleNext() again itself
 * after each run, so every single run's delay is freshly computed from
 * the actual current time — any drift self-corrects instead of stacking.
 */
public final class MidnightScheduler {

    private static final String WORK_NAME = "midnight_reset";

    private MidnightScheduler() {}

    public static void scheduleNext(Context context) {
        Calendar nextMidnight = Calendar.getInstance();
        nextMidnight.add(Calendar.DAY_OF_YEAR, 1);
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 1);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);

        long delay = nextMidnight.getTimeInMillis() - System.currentTimeMillis();
        if (delay <= 0) delay = TimeUnit.MINUTES.toMillis(1); // safety net, shouldn't happen

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MidnightResetWorker.class)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build();

        // REPLACE (not KEEP): if the app restarts before midnight, we want
        // the freshest possible delay calculation, not a stale queued one.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }
}

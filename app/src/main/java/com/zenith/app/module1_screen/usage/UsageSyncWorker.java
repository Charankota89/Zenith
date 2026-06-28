package com.zenith.app.module1_screen.usage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zenith.app.data.repository.UsageRepository;
import com.zenith.app.utils.Constants;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager Worker that syncs today's usage data from UsageStatsManager
 * into the Room DB every 15 minutes.
 *
 * Schedule this from MainActivity or ZenithApp.
 */
public class UsageSyncWorker extends Worker {

    public UsageSyncWorker(@NonNull Context context,
                           @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        UsageRepository repository = new UsageRepository(getApplicationContext());

        if (!repository.hasUsagePermission()) {
            // Can't sync without permission — try again next interval
            return Result.retry();
        }

        repository.syncTodayUsage();

        // Also clean up old records
        repository.deleteOldRecords();

        return Result.success();
    }

    /**
     * Call once (e.g. from ZenithApp or MainActivity) to schedule the periodic sync.
     * Uses KEEP policy so re-scheduling after reboot doesn't create duplicates.
     */
    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                UsageSyncWorker.class,
                Constants.USAGE_SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        )
        .addTag(Constants.WORK_USAGE_SYNC)
        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_USAGE_SYNC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}

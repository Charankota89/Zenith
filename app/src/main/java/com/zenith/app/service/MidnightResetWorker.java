package com.zenith.app.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.db.AppDatabase;
import com.zenith.app.util.TimeUtils;

public class MidnightResetWorker extends Worker {

    public MidnightResetWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db    = AppDatabase.getInstance(getApplicationContext());
        String      today = TimeUtils.getTodayDate();
        db.appUsageDao().unlockAllExceptToday(today);
        db.habitDao().resetDailyCompletion();
        return Result.success();
    }
}

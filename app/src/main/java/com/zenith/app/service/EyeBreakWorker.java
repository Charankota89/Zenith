package com.zenith.app.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.util.NotificationHelper;

public class EyeBreakWorker extends Worker {

    public EyeBreakWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationHelper.notifyEyeBreak(getApplicationContext());
        return Result.success();
    }
}

package com.zenith.app.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.util.NotificationHelper;

public class PostureWorker extends Worker {

    public PostureWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationHelper.notifyPostureCheck(getApplicationContext());
        return Result.success();
    }
}

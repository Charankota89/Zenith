package com.zenith.app.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.zenith.app.ui.MainActivity;
import com.zenith.app.util.AppConstants;

public class PostureWorker extends Worker {

    public PostureWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent intent    = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
            intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(
            getApplicationContext(), AppConstants.CHANNEL_ID_WELLBEING)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Posture check")
            .setContentText("Sit straight, roll your shoulders. 30 seconds is all it takes.")
            .setAutoCancel(true).setContentIntent(pi);

        ((NotificationManager) getApplicationContext()
            .getSystemService(Context.NOTIFICATION_SERVICE))
            .notify(AppConstants.NOTIF_ID_POSTURE, b.build());
        return Result.success();
    }
}

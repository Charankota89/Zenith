package com.zenith.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.zenith.app.service.UsageMonitorService;
import com.zenith.app.util.MidnightScheduler;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            // Re-start the foreground usage monitor service so screen-time
            // tracking resumes immediately after a device reboot.
            try {
                Intent service = new Intent(context, UsageMonitorService.class);
                context.startForegroundService(service);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Re-schedule the midnight reset worker. The WorkManager queue is
            // cleared when the device powers off, so without this call the
            // midnight reset would never fire again after a reboot until the
            // user manually opens the app. Rescheduling here ensures the reset
            // always fires at the correct next midnight regardless of reboots.
            MidnightScheduler.scheduleNext(context);
        }
    }
}

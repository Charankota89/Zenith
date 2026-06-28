package com.zenith.app.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zenith.app.module1_screen.notification.ZenithForegroundService;

/**
 * Boot completed receiver — restarts the monitoring foreground service
 * after device reboot so timers and overlays keep working.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Intent serviceIntent = new Intent(context, ZenithForegroundService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}

package com.example.travelog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Cihaz yeniden başlatıldığında günlük alarmı yeniden ayarlar. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationHelper.createChannel(context);
            NotificationHelper.scheduleDailyAlarm(context);
        }
    }
}

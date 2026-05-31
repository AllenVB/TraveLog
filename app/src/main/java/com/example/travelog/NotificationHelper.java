package com.example.travelog;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class NotificationHelper {

    public static final String CHANNEL_ID = "travelog_daily";
    public static final String CHANNEL_NAME = "Günlük Anımsatma";
    private static final int ALARM_REQUEST_CODE = 42;

    /** Bildirim kanalını oluştur (Android 8+). İdempotent — güvenle tekrar çağrılabilir. */
    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Her gün 'Bu gün yıl önce' anımsatmaları");
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    /** Her gün saat 09:00'da tetiklenecek tekrarlayan alarmı ayarla. */
    public static void scheduleDailyAlarm(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, OnThisDayReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        // Bugün saat geçtiyse yarına koy
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }
}

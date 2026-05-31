package com.example.travelog;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Her gün 09:00'da tetiklenir. O günün gün+ay kısmıyla eşleşen anılar varsa bildirim gönderir.
 */
public class OnThisDayReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Bugünün "dd.MM" kısmı
        String todayDayMonth = new SimpleDateFormat("dd.MM", Locale.getDefault())
                .format(new Date());

        // DB'yi bg thread'de sorgula (BroadcastReceiver kısa süre yaşar, goAsync kullan)
        BroadcastReceiver.PendingResult result = goAsync();
        new Thread(() -> {
            try {
                List<Memory> all = AppDatabase.getInstance(context).memoryDao().getAllMemories();
                Memory match = null;
                for (Memory m : all) {
                    if (m.date != null && m.date.startsWith(todayDayMonth)) {
                        match = m;
                        break;
                    }
                }
                if (match != null) sendNotification(context, match);

                // Gelecek planları için hatırlatma (bugün / yarın)
                checkFuturePlans(context);
            } finally {
                result.finish();
            }
        }).start();
    }

    /** Tarihi bugün veya yarın olan gezi planları için bildirim gönder. */
    private void checkFuturePlans(Context context) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String todayStr = fmt.format(new Date());
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowStr = fmt.format(tomorrow.getTime());

        List<Memory> plans = AppDatabase.getInstance(context).memoryDao().getFuturePlans();
        for (Memory plan : plans) {
            if (plan.date == null) continue;
            if (plan.date.equals(todayStr)) {
                sendPlanNotification(context, plan, "Bugün gezi planın var! 🎉");
            } else if (plan.date.equals(tomorrowStr)) {
                sendPlanNotification(context, plan, "Yarın gezin var! Hazırlan ✈");
            }
        }
    }

    private void sendPlanNotification(Context context, Memory plan, String title) {
        String city = plan.city != null ? plan.city : "";
        String body = city + " — \"" + (plan.title != null ? plan.title : "") + "\" ("
                + plan.date + ")";

        Intent openIntent = new Intent(context, DetailActivity.class);
        openIntent.putExtra("memory", plan);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Plan bildirimleri için ayrı id alanı (anı id'leriyle çakışmasın)
        int notifId = 100000 + plan.id;
        PendingIntent pi = PendingIntent.getActivity(context, notifId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(notifId, builder.build());
    }

    private void sendNotification(Context context, Memory memory) {
        // Tarihin yılını çıkar → "yıl önce" hesabı
        String yearAgo = "";
        try {
            String[] parts = memory.date.split("\\.");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[2]);
                int diff = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - year;
                yearAgo = diff > 0 ? diff + " yıl önce" : "bugün";
            }
        } catch (Exception ignored) {}

        String title = "Bu Gun Gecmiste";
        String body  = yearAgo + " " + memory.city + "'deydin! \"" + memory.title + "\"";

        Intent openIntent = new Intent(context, DetailActivity.class);
        openIntent.putExtra("memory", memory);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, memory.id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_map)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(memory.id, builder.build());
    }
}

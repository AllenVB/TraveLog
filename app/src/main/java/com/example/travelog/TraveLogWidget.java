package com.example.travelog;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.List;
import java.util.concurrent.Executors;

/** Ana ekran widget'ı — toplam anı sayısını ve son şehri gösterir. */
public class TraveLogWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager awm, int[] appWidgetIds) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            int total = db.memoryDao().getTotalCount();
            int cities = db.memoryDao().getUniqueCityCount();
            List<Memory> recent = db.memoryDao().getAllMemories();
            String lastCity = recent.isEmpty() ? "—" : recent.get(0).city;

            for (int id : appWidgetIds) {
                RemoteViews rv = new RemoteViews(
                        context.getPackageName(), R.layout.widget_travelog);
                rv.setTextViewText(R.id.widgetTotalMemories, total + " anı");
                rv.setTextViewText(R.id.widgetCities, cities + " şehir");
                rv.setTextViewText(R.id.widgetLastCity, "📍 " + lastCity);

                // Tıklayınca uygulamayı aç
                Intent launch = new Intent(context, MainActivity.class);
                PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                rv.setOnClickPendingIntent(R.id.widgetRoot, pi);

                awm.updateAppWidget(id, rv);
            }
        });
    }
}

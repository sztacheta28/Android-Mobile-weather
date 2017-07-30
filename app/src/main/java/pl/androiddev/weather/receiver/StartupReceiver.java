package pl.androiddev.weather.receiver;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import pl.androiddev.weather.preferences.Prefs;
import pl.androiddev.weather.service.NotificationService;
import pl.androiddev.weather.widget.LargeWidgetProvider;
import pl.androiddev.weather.widget.SmallWidgetProvider;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent2 = new Intent(context, LargeWidgetProvider.class);
        intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context , LargeWidgetProvider.class));
        intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        context.sendBroadcast(intent2);

        intent2 = new Intent(context, SmallWidgetProvider.class);
        intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context , SmallWidgetProvider.class));
        intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        context.sendBroadcast(intent2);

        NotificationService.setNotificationServiceAlarm(context , new Prefs(context).getNotifs());
    }
}


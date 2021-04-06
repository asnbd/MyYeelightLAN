package me.asswad.myyeelightlan.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import me.asswad.myyeelightlan.LightControl;
import me.asswad.myyeelightlan.MainActivity;
import me.asswad.myyeelightlan.R;

/**
 * Implementation of App Widget functionality.
 */
public class LightControlWidget extends AppWidgetProvider {
    private final String TAG = "LightControlWidget";

    private static final String ACTION_UPDATE_CLICK_ON = "action.UPDATE_CLICK_ON";
    private static final String ACTION_UPDATE_CLICK_OFF = "action.UPDATE_CLICK_OFF";
    private static final String ACTION_UPDATE_CLICK_TOGGLE = "action.UPDATE_CLICK_TOGGLE";
    private static final String ACTION_UPDATE_CLICK_BRIGHT = "action.UPDATE_CLICK_BRIGHT";
    private static final String ACTION_UPDATE_CLICK_MEDIUM = "action.UPDATE_CLICK_MEDIUM";
    private static final String ACTION_UPDATE_CLICK_DIM = "action.UPDATE_CLICK_DIM";

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.light_control_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);

        views.setOnClickPendingIntent(R.id.w_btn_on, getPendingSelfIntent(context, ACTION_UPDATE_CLICK_ON));
        views.setOnClickPendingIntent(R.id.w_btn_off, getPendingSelfIntent(context, ACTION_UPDATE_CLICK_OFF));
        views.setOnClickPendingIntent(R.id.w_btn_dim, getPendingSelfIntent(context, ACTION_UPDATE_CLICK_DIM));
        views.setOnClickPendingIntent(R.id.w_btn_medium, getPendingSelfIntent(context, ACTION_UPDATE_CLICK_MEDIUM));
        views.setOnClickPendingIntent(R.id.w_btn_bright, getPendingSelfIntent(context, ACTION_UPDATE_CLICK_BRIGHT));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        switch (intent.getAction()){
            case ACTION_UPDATE_CLICK_ON:
                Log.d(TAG, "onReceive: ON CLICKED");
                new LightControl(context).turnOn();
                break;

            case ACTION_UPDATE_CLICK_OFF:
                Log.d(TAG, "onReceive: OFF CLICKED");
                new LightControl(context).turnOff();
                break;

            case ACTION_UPDATE_CLICK_DIM:
                Log.d(TAG, "onReceive: DIM CLICKED");
                new LightControl(context).dim();
                break;

            case ACTION_UPDATE_CLICK_MEDIUM:
                Log.d(TAG, "onReceive: MEDIUM CLICKED");
                new LightControl(context).medium();
                break;

            case ACTION_UPDATE_CLICK_BRIGHT:
                Log.d(TAG, "onReceive: BRIGHT CLICKED");
                new LightControl(context).bright();
                break;
        }

    }

    private PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass()); // An intent directed at the current class (the "self").
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
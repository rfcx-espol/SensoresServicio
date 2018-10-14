package com.example.jorge.blue.servicios;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import static com.example.jorge.blue.utils.Identifiers.delta_time;
import static com.example.jorge.blue.utils.Identifiers.pendingIntentSending;
import static com.example.jorge.blue.utils.Identifiers.pendingIntentReceiver;
import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.onSendingService;
import static com.example.jorge.blue.utils.Identifiers.onServiceReceiver;

public class ReceiverCall extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //INICIAR LOS SERVICIOS
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            if (!onSendingService) {
                pendingIntentSending = PendingIntent.getService(context, 0,
                        new Intent(context, SendingService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000,
                            delta_time, pendingIntentSending);
                }
                onSendingService = true;
            }
            if (!onServiceReceiver) {
                pendingIntentReceiver = PendingIntent.getService(context, 0,
                        new Intent(context, ServiceReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000,
                            delta_time, pendingIntentReceiver);
                }
                onServiceReceiver = true;
            }
        }
    }

}
package com.example.jorge.blue.servicios;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;


import android.util.Log;

import static com.example.jorge.blue.utils.Identifiers.delta_time;
import static com.example.jorge.blue.utils.Identifiers.pendingIntent;
import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import android.widget.Toast;



import static com.example.jorge.blue.utils.Identifiers.onService;
import static com.example.jorge.blue.utils.Identifiers.onService2;

import java.io.File;
import java.util.Calendar;

/**
 * Created by JORGE on 5/6/18.
 */

public class ReceiverCall extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.i("SERVICIO", "REINICIANDO EL SERVICIO");
        //INICIAR EL SERVICIO
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            Log.d("ENv", "Servicio Creado");

            if (!onService2) {
                Toast.makeText(context, "Servicio iniciado (boot)", Toast.LENGTH_LONG).show();
                Intent myIntent = new Intent(context, ServiceReceiver.class);
                context.startService(myIntent);


                onService2 = true;
            }

            Log.d("hi", "Servicio Creado");
        }

    }
}

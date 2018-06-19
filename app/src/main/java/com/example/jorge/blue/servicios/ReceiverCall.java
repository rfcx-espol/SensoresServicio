package com.example.jorge.blue.servicios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.widget.Toast;


import static com.example.jorge.blue.utils.Identifiers.onService;

import java.io.File;

/**
 * Created by JORGE on 5/6/18.
 */

public class ReceiverCall extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.i("SERVICIO", "REINICIANDO EL SERVICIO");
        //INICIAR EL SERVICIO
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            if (!onService) {
                Toast.makeText(context, "Servicio iniciado (boot)", Toast.LENGTH_LONG).show();
                Intent myIntent = new Intent(context, ServiceReceiver.class);
                context.startService(myIntent);
                onService = true;
            }
        }
    }
}

package com.example.jorge.blue.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;


/**
 * Created by JORGE on 5/6/18.
 */

public class Identifiers {
    public static boolean onService = false;
    //INTENT EN EJECUCIÓN
    public static PendingIntent pendingIntent;
    //ALARMA EN EJECUCIÓN
    public static AlarmManager alarmManager;

    public static final String URL_SERVER ="http://200.126.14.250/api/InfoSensores";


}

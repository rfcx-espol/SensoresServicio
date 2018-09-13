package com.example.jorge.blue.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import java.util.Map;
import okhttp3.Call;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;


/**
 * Created by JORGE on 5/6/18.
 */

public class Identifiers {
    public static boolean onService = false;
    public static boolean onService2 = false;
    //INTENT EN EJECUCIÓN
    public static PendingIntent pendingIntent;
    //ALARMA EN EJECUCIÓN
    public static AlarmManager alarmManager;

    public static final String URL_SERVER ="http://200.126.14.250/api/Data";

    public static String APIKey;
    public static Call call;
    public static boolean threadRunning = true;
    public static String ID_STATION;
    public static int delta_time =  60 * 1000;   //60 seg
    public static String BT_address = "";

    @SuppressLint("HardwareIds")
    public static void setAPIKey(Context context){
        APIKey = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }


}

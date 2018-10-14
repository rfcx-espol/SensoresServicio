package com.example.jorge.blue.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import okhttp3.Call;
import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

public class Identifiers {
    public static boolean onSendingService = false;
    public static boolean onServiceReceiver = false;
    public static PendingIntent pendingIntentSending;
    public static PendingIntent pendingIntentReceiver;
    public static AlarmManager alarmManager;
    public static final String URL_SERVER ="http://200.126.14.250/api/Data";
    public static String APIKey;
    public static Call callSending;
    public static Call callReceiver;
    public static boolean threadRunning = true;
    public static String ID_STATION;
    public static int delta_time =  60000;
    public static String BT_address = "";

    @SuppressLint("HardwareIds")
    public static void setAPIKey(Context context){
        APIKey = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
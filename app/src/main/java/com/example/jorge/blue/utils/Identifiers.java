package com.example.jorge.blue.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import okhttp3.Call;
import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import com.example.jorge.blue.entidades.ConexionSQLiteHelper;

import java.util.UUID;

public class Identifiers {
    public static boolean onSendingService = false;
    public static boolean onServiceReceiver = false;
    public static PendingIntent pendingIntentSending;
    public static PendingIntent pendingIntentReceiver;
    public static AlarmManager alarmManager;
    public static String APIKey;
    public static Call callSending;
    public static Call callReceiver;
    public static boolean threadRunning = true;
    public static String ID_STATION;
    public static UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String BT_address = "";
    public static ConexionSQLiteHelper connection;
    public static final String URL_SERVER ="http://200.126.14.250/api/Data";
    public static final String TABLA_MEDICION = "medicion";
    public static final String CAMPO_TIMESTAMP = "timestamp";
    public static final String CAMPO_TYPE = "type";
    public static final String CAMPO_VALUE = "value";
    public static final String CAMPO_UNIT = "unit";
    public static final String CAMPO_LOCATION = "location";
    public static final String CAMPO_SENSORID = "sensor";
    public final static String CREAR_TABLA_MEDICION = "CREATE TABLE " + TABLA_MEDICION +
            "("+ CAMPO_TIMESTAMP +" TEXT, " + CAMPO_TYPE + " TEXT, " + CAMPO_VALUE +
            " REAL, " + CAMPO_UNIT + " TEXT, " + CAMPO_SENSORID + " TEXT, " + CAMPO_LOCATION + " TEXT)";

    @SuppressLint("HardwareIds")
    public static void setAPIKey(Context context){
        APIKey = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
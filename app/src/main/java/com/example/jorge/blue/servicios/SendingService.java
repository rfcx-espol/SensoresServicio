package com.example.jorge.blue.servicios;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.entidades.medicion;
import com.example.jorge.blue.utils.Identifiers;
import com.example.jorge.blue.utils.Utilities;

import okhttp3.OkHttpClient;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;



public class SendingService extends Service {
    public static PowerManager.WakeLock wakeLock;
    private final IBinder mBinder = new LocalBinder();
    private static OkHttpClient okHttpClient = new OkHttpClient();
    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "mediciones", null, 1);

    public class LocalBinder extends Binder {
        public SendingService getService() {
            return SendingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {

        //MANTENER ENCENDIDO EL CPU DEL CELULAR AL APAGAR LA PANTALLA
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendPost();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }



    //CREAR BUILDER DEL CLIENTE PARA QUE EXPIRE DESPUÉS DE 10 MINUTOS SIN CONEXIÓN
    public static void builder(){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES);
        okHttpClient = builder.build();
    }

    public JSONArray consultarMediciones()
    {
        SQLiteDatabase db = conn.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + Utilities.TABLA_MEDICION, null);
        JSONArray jsonArray = new JSONArray();


        try {
            while (cursor.moveToNext())
            {
                JSONObject j = new JSONObject();
                j.put("timestamp", cursor.getString(0));
                j.put("type", cursor.getString(1));
                j.put("value", cursor.getString(2));
                j.put("unit", cursor.getString(3));
                j.put("location", cursor.getString(4));
                jsonArray.put(j);
            }

        }catch (Exception e)
        {
            Log.d("DB", "no se pudo cargar datos desde la base");
        }
        conn.close();
        return jsonArray;
    }

    public void sendPost() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int x = 0;
                try {
                    URL url = new URL(Identifiers.URL_SERVER);
                    HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                    connect.setRequestMethod("POST");
                    connect.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connect.setRequestProperty("Accept","application/json");
                    connect.setDoOutput(true);
                    connect.setDoInput(true);

                    JSONArray jsonParam = consultarMediciones();

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(connect.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(connect.getResponseCode()));
                    Log.i("MSG" , connect.getResponseMessage());

                    connect.disconnect();
                    x = 1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (x == 1)
                {
                    borrarBD();
                }
            }
        });

        thread.start();
    }

    public void borrarBD()
    {
        SQLiteDatabase db = conn.getReadableDatabase();
        Cursor cursor = db.rawQuery("DELETE FROM " + Utilities.TABLA_MEDICION, null);
        conn.close();
    }

}

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
import com.example.jorge.blue.utils.Identifiers;
import com.example.jorge.blue.utils.Utilities;

import okhttp3.OkHttpClient;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;



public class SendingService extends Service {
    public static PowerManager.WakeLock wakeLock;
    private final IBinder mBinder = new LocalBinder();
    String st;
    private static OkHttpClient okHttpClient = new OkHttpClient();
    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "medicion", null, 1);

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
        Log.d("SS", "Servicio Envio ejecutado");



        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }


    public JSONObject consultarMediciones()
    {
        SQLiteDatabase db = conn.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + Utilities.TABLA_MEDICION, null);
        JSONArray jsonArray = new JSONArray();
        JSONObject y = new JSONObject();
        int c = 0;

        boolean responseId = Utilities.getStationID(okHttpClient);


        if(responseId) {

            try {
                while (cursor.moveToNext()) {
                    JSONObject j = new JSONObject();

                    j.put("StationId", Identifiers.ID_STATION);

                    j.put("Timestamp", cursor.getString(0));
                    j.put("Type", cursor.getString(1));
                    j.put("Value", cursor.getString(2));
                    j.put("Units", cursor.getString(3));
                    j.put("Location", cursor.getString(5));
                    j.put("SensorId", cursor.getString(4));
                    jsonArray.put(j);
                    //                Long tsLong = System.currentTimeMillis()/1000;
                    //                String ts = tsLong.toString();
                    //                j.put("StationId", "10");
                    //                j.put("SensorId", c.toString());
                    //                j.put("Timestamp", ts);
                    //                j.put("Type", "Temperature");
                    //                j.put("Value", cursor.getString(2));
                    //                j.put("Units", "Celsius");
                    //                j.put("Location", "Enviroment");
                    //                jsonArray.put(j);

                    c++;
//                    if (c==3)
//                    {
//                        break;
//                    }

                }

                y.put("data", jsonArray);
            } catch (Exception e) {
                Log.d("DB", "no se pudo cargar datos desde la base");
            }
            conn.close();
            return y;
        }
        return null;
    }

    public void sendPost() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    URL url = new URL(Identifiers.URL_SERVER);
                    HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                    connect.setRequestMethod("POST");
                    connect.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connect.setRequestProperty("Accept","application/json");
                    connect.setDoOutput(true);
                    connect.setDoInput(true);

                    JSONObject jsonParam = consultarMediciones();

                    Log.d("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(connect.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    st = String.valueOf(connect.getResponseCode());
                    Log.d("STATUS", st);
                    Log.d("MSG" , connect.getResponseMessage());

                    connect.disconnect();
                    Thread.sleep(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("SS", "Error al  enviar datos");

                }
                if (st == "200")
                {
                    borrarBD();
                    Log.d("SS", "Data enviada y borrada");
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

package com.example.jorge.blue.servicios;

import android.annotation.SuppressLint;
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
    private final IBinder mBinder = new LocalBinder();
    private final String TAG = "SENDING SERVICE";
    private boolean responseId;
    private OkHttpClient okHttpClient;
    private ConexionSQLiteHelper connection;
    public static PowerManager.WakeLock wakeLock;
    public static Thread thread;

    public class LocalBinder extends Binder {
        public SendingService getService() {
            return SendingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        responseId = Utilities.getStationID(okHttpClient);
        Identifiers.setAPIKey(getApplicationContext());
        connection = new ConexionSQLiteHelper(getApplicationContext(), "medicion", null, 1);
        okHttpClient = new OkHttpClient();

        //MANTENER ENCENDIDO EL CPU DEL CELULAR AL APAGAR LA PANTALLA
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        responseId = Utilities.getStationID(okHttpClient);
        //sendPost();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }

    public void sendPost() {
        thread = new Thread(new Runnable() {
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
                    Log.i(TAG, "DATOS A ENVIAR AL SERVIDOR: " + jsonParam.toString());

                    DataOutputStream os = new DataOutputStream(connect.getOutputStream());
                    os.writeBytes(jsonParam.toString());
                    os.flush();
                    os.close();

                    int st = connect.getResponseCode();
                    Log.d(TAG, "CÓDIGO DE RESPUESTA DEL SERVIDOR: " + String.valueOf(st));
                    //Log.d("MSG" , connect.getResponseMessage());

                    connect.disconnect();
                    if (st == 200) {
                        borrarBD();
                        Log.i(TAG, "DATOS ENVIADOS Y BORRADOS");
                    }
                    //Thread.sleep(10000);
                } catch (Exception e) {
                    Log.e(TAG, "ERROR DE ENVÍO DE DATOS: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    public JSONObject consultarMediciones() {
        SQLiteDatabase db = connection.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + Identifiers.TABLA_MEDICION, null);
        db.close();
        JSONArray jsonArray = new JSONArray();
        JSONObject y = new JSONObject();
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
                }
                y.put("data", jsonArray);
            } catch (Exception e) {
                Log.e(TAG, "ERROR AL CARGAR DATOS DE LA BASE DEL DISPOSITIV: " + e.getMessage());
                e.printStackTrace();
            }
            connection.close();
            return y;
        }
        return null;
    }

    public void borrarBD() {
        SQLiteDatabase db = connection.getReadableDatabase();
        db.execSQL("DELETE FROM " + Identifiers.TABLA_MEDICION);
        connection.close();
        Log.i(TAG, "DATOS BORRADOS DE LA BASE: " + Identifiers.TABLA_MEDICION);
    }

}
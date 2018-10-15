package com.example.jorge.blue.servicios;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.example.jorge.blue.activities.UserInterfaz;
import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.utils.Identifiers;
import com.example.jorge.blue.utils.Utilities;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class SendingService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private final String TAG = "SENDING SERVICE";
    private boolean responseId;
    private OkHttpClient okHttpClient;
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
        boolean res = false;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(300, TimeUnit.SECONDS);
        builder.readTimeout(300, TimeUnit.SECONDS);
        builder.writeTimeout(300, TimeUnit.SECONDS);
        okHttpClient = builder.build();

        Identifiers.connection = new ConexionSQLiteHelper(this, "medicion", null, 1);
        while(!res) {
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                responseId = Utilities.getStationID(okHttpClient);
                res = true;
            } else {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "NO HAY CONEXIÓN A INTERNET. SE VOLVERÁ A INTENTAR EN 60 SEGUNDOS");
                    e.printStackTrace();
                }
            }
        }
        Identifiers.setAPIKey(getApplicationContext());

        //MANTENER ENCENDIDO EL CPU DEL CELULAR AL APAGAR LA PANTALLA
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        responseId = Utilities.getStationID(okHttpClient);
        sendPost();
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
                boolean res = false;
                while(!res) {
                    try {
                        Log.i(TAG, "ENTRO");

                        OkHttpClient.Builder builder = new OkHttpClient.Builder();
                        builder.connectTimeout(300, TimeUnit.SECONDS);
                        builder.readTimeout(300, TimeUnit.SECONDS);
                        builder.writeTimeout(300, TimeUnit.SECONDS);
                        okHttpClient = builder.build();

                        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                        JSONObject jsonParam = consultarMediciones();
                        Log.i(TAG, "DATOS A ENVIAR AL SERVIDOR: " + jsonParam.toString());
                        RequestBody body = RequestBody.create(JSON, String.valueOf(jsonParam));
                        Request request = new Request.Builder()
                                .url(Identifiers.URL_SERVER)
                                .post(body)
                                .build();
                        Identifiers.callSending = okHttpClient.newCall(request);
                        borrarBD();
                        Log.i(TAG, "DATOS BORRADOS Y ENVIÁNDOSE");
                        Identifiers.callSending.execute();
                        /*int respuesta = response.code();
                        Log.i(TAG, "CÓDIGO DE RESPUESTA DEL SERVIDOR: " + String.valueOf(respuesta));*/

                        /*URL url = new URL(Identifiers.URL_SERVER);
                        HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                        connect.setRequestMethod("POST");
                        connect.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        connect.setRequestProperty("Accept", "application/json");
                        connect.setDoOutput(true);
                        connect.setDoInput(true);

                        JSONObject jsonParam = consultarMediciones();
                        Log.i(TAG, "DATOS A ENVIAR AL SERVIDOR: " + jsonParam.toString());
                        borrarBD();
                        Log.i(TAG, "DATOS ENVIADOS Y BORRADOS");

                        DataOutputStream os = new DataOutputStream(connect.getOutputStream());
                        os.writeBytes(jsonParam.toString());
                        os.flush();
                        os.close();

                        int st = connect.getResponseCode();
                        Log.i(TAG, "CÓDIGO DE RESPUESTA DEL SERVIDOR: " + String.valueOf(st));

                        connect.disconnect();*/
                        Thread.sleep(5000);
                    } catch(Exception e) {
                        Log.e(TAG, "ERROR AL ENVIAR LOS DATOS: " + e.getMessage());
                        e.printStackTrace();
                        res = true;
                    }
                }
            }
        });

        thread.start();
    }

    public JSONObject consultarMediciones() {
        SQLiteDatabase db = Identifiers.connection.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM " + Identifiers.TABLA_MEDICION, null);
                JSONArray jsonArray = new JSONArray();
                JSONObject y = new JSONObject();
                if(responseId) {
                    try {
                        while(cursor.moveToNext()) {
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
                        Log.e(TAG, "ERROR AL CARGAR DATOS DE LA BASE DEL DISPOSITIVO: " + e.getMessage());
                        e.printStackTrace();
                    }
                    db.close();
                    Identifiers.connection.close();
                    return y;
                }
        return null;
    }

    public void borrarBD() {
        SQLiteDatabase db = Identifiers.connection.getReadableDatabase();
        db.execSQL("DELETE FROM " + Identifiers.TABLA_MEDICION);
        Identifiers.connection.close();
        Log.i(TAG, "DATOS BORRADOS DE LA BASE: " + Identifiers.TABLA_MEDICION);
    }

}
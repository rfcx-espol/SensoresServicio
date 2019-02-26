package com.example.jorge.blue.servicios;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.JsonReader;
import android.util.Log;

import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.utils.Identifiers;
import com.example.jorge.blue.utils.ImageSender;
import com.example.jorge.blue.utils.Utilities;

import okhttp3.OkHttpClient;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.example.jorge.blue.utils.Identifiers.setAPIKey;


public class SendingService extends Service {
    public static PowerManager.WakeLock wakeLock;
    public static String TAG = "USB-Service";
    private final IBinder mBinder = new LocalBinder();
    int st;
    boolean responseId;

    public static String STATION_LOG = "SENDING DATA: ";
    private static OkHttpClient okHttpClient = new OkHttpClient();
    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, Utilities.MEASURE_DATABASE_NAME, null, 1);

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
        //responseId = Utilities.getStationID(okHttpClient);
        setAPIKey(getApplicationContext());
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here
            responseId = Utilities.getStationID(okHttpClient);
        }

        sendPost();
        try{
            autoSendPhoto();
        }catch (Exception e){
            e.printStackTrace();
        }

        Log.d("SS", "Servicio Envio ejecutado");

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }


    public JSONObject getImages()
    {
        SQLiteDatabase db = conn.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + Utilities.IMAGES_TABLE+" ORDER BY " +Utilities.IMAGE_TIMESTAMP+" LIMIT "+Utilities.LIMIT_BY_DEFAULT_FOR_IMAGES, null);
        JSONArray jsonArray = new JSONArray();
        JSONObject y = new JSONObject();

        if(responseId) {

            try {
                while (cursor.moveToNext()) {
                    JSONObject j = new JSONObject();

                    j.put("id", cursor.getInt(0));
                    j.put("Name", cursor.getString(1));
                    j.put("Type", cursor.getString(2));
                    j.put("Timestamp", cursor.getString(3));
                    jsonArray.put(j);

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


    public JSONObject getMeasures()
    {
        SQLiteDatabase db = conn.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + Utilities.MEASURE_TABLE+" ORDER BY " +Utilities.FIELD_TIMESTAMP+" LIMIT "+Utilities.LIMIT_BY_DEFAULT, null);
        JSONArray jsonArray = new JSONArray();
        JSONObject y = new JSONObject();

        if(responseId) {

            try {
                while (cursor.moveToNext()) {
                    JSONObject j = new JSONObject();
                    Log.d("DB", "DATOS: "+ cursor.getString(3));

                    j.put("StationId", Identifiers.ID_STATION);
                    j.put("id", cursor.getInt(0));
                    j.put("Timestamp", cursor.getString(1));
                    j.put("Type", cursor.getString(2));
                    j.put("Value", cursor.getString(3));
                    j.put("Units", cursor.getString(4));
                    j.put("Location", cursor.getString(5));
                    j.put("SensorId", cursor.getString(6));
                    jsonArray.put(j);
                }
                y.put("data", jsonArray);
                Log.d("DB", "DATOS: "+ jsonArray.toString());

            } catch (Exception e) {
                Log.d("DB", "no se pudo cargar datos desde la base");
            }
            conn.close();
            return y;
        }
        return null;
    }

    public void autoSendPhoto(){
        JSONObject jsonParam = getImages();
        try {
            JSONArray data = jsonParam.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                String nameComposed = data.getJSONObject(i).getString("Name");
                int id = data.getJSONObject(i).getInt("id");
                String unixtime = data.getJSONObject(i).getString("Timestamp");
                Log.d(STATION_LOG, "SENDING PHOTO ID: "+id);
                Log.d(STATION_LOG, "SENDING PHOTO NAME: "+nameComposed);
                Log.d(STATION_LOG, "SENDING PHOTO UNIXTIME: "+unixtime);
                sendPhoto(this, id, nameComposed, unixtime);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendPhoto(Context context, int id, String photoName, String unixtime){

        String extStorage = Environment.getExternalStorageDirectory().toString();
        File photo = new File(extStorage, photoName);

        boolean exists =  photo.exists();

        new ImageSender(context,
                id, photo, unixtime).execute();
    }

    public void sendPost() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(STATION_LOG, "ANDROID API KEY: "+Identifiers.APIKey);
                Log.d(STATION_LOG, "STATION ID: "+Identifiers.ID_STATION);

                try {
                    String urlComposed = Identifiers.URL_SERVER+"api/Data";
                    URL url = new URL(urlComposed);
                    HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                    connect.setRequestMethod("POST");
                    connect.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connect.setRequestProperty("Accept","application/json");
                    connect.setDoOutput(true);
                    connect.setDoInput(true);

                    JSONObject jsonParam = getMeasures();

                    Log.d(STATION_LOG, "DATA TO SEND: "+jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(connect.getOutputStream());
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();
                    st = connect.getResponseCode();
                    Log.d(STATION_LOG, "RESPONSE STATUS CODE: "+String.valueOf(st));
                    Log.d(STATION_LOG, "RESPONSE MESSAGE: "+connect.getResponseMessage());
                    connect.disconnect();
                    if (st == 200)
                    {
                        Log.d(STATION_LOG, "UPLOADED WITH SUCCESS!!");
                        deleteMeasures(jsonParam.getJSONArray("data"));
                        Log.d(STATION_LOG, "DATA DELETED!!");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(STATION_LOG, "ERROR: "+e.getMessage());

                }
            }
        });

        thread.start();
    }

    public void deleteMeasures(JSONArray data) {
        try{

            List<Integer> ids = new ArrayList<Integer>();

            for (int i = 0; i< data.length(); i++) {
                int id = data.getJSONObject(i).getInt("id");

                ids.add(id);
            }
            SQLiteDatabase db = conn.getReadableDatabase();

            StringBuilder b = new StringBuilder("DELETE FROM " + Utilities.MEASURE_TABLE +" WHERE "+Utilities.FIELD_ID+" IN(" );
            String[] whereArgs = new String[ids.size()];
            int index = 0;
            for (int id: ids) {
                whereArgs[index] = String.valueOf(id);
                b.append("?");
                if (index < ids.size() - 1) {
                    b.append(",");
                }
                index++;
            }
            b.append(")");

            db.execSQL(b.toString(), whereArgs);

            Log.d(TAG,"DELETING MEASURE IDS: "+whereArgs.toString());
            conn.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}

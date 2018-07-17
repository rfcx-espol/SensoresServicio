package com.example.jorge.blue.utils;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.media.MediaMetadataRetriever;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Date;
import java.text.SimpleDateFormat;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Callback;
import static com.example.jorge.blue.utils.Identifiers.call;
import static com.example.jorge.blue.utils.Identifiers.threadRunning;


/**
 * Created by JORGE on 4/6/18.
 */

public class Utilities {
    //Constantes campos tabla medicion

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

    //MÉTODO QUE ENVÍA EL APIKEY Y RECIBE EL ID DE LA ESTACIÓN EN LA BASE DE DATOS
    public static boolean getStationID(OkHttpClient okHttpClient){
        HttpUrl.Builder httpBuilder = HttpUrl.parse("http://200.126.14.250/api/Station").newBuilder();
        httpBuilder.addQueryParameter("APIKey", Identifiers.APIKey);
        Log.d("HTTP", String.valueOf(httpBuilder));
        Request request = new Request.Builder().url(httpBuilder.build()).build();
        call = okHttpClient.newCall(request);
        try {
            if(!threadRunning){
                return false;
            }
            Response response = call.execute();
            if(response.code() == 200){
                String resp = response.body().string();
                //Log.d("RESP", resp);
                JSONObject obj = new JSONObject(resp);
                if(obj.getString("APIKey").equals(Identifiers.APIKey)){
                    Identifiers.ID_STATION = obj.getString("Id");
                    response.body().close();
                    return true;
                }
                return false;
            } else {
                response.body().close();
                return false;
            }
        } catch(IOException e){
            e.printStackTrace();
            return false;
        } catch(org.json.JSONException je){
            je.printStackTrace();
            Log.d("ERROR", je.getMessage());
            return false;
        }
    }



}

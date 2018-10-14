package com.example.jorge.blue.utils;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import static com.example.jorge.blue.utils.Identifiers.callSending;
import static com.example.jorge.blue.utils.Identifiers.callReceiver;
import static com.example.jorge.blue.utils.Identifiers.threadRunning;

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
        Request request = new Request.Builder().url(httpBuilder.build()).build();
        callSending = okHttpClient.newCall(request);
        try {
            if(!threadRunning){
                return false;
            }
            Response response = callSending.execute();
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
            return false;
        }
    }

}
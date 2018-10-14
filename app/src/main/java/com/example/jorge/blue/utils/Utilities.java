package com.example.jorge.blue.utils;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Utilities {

    //MÉTODO QUE ENVÍA EL APIKEY Y RECIBE EL ID DE LA ESTACIÓN EN LA BASE DE DATOS
    public static boolean getStationID(OkHttpClient okHttpClient){
        HttpUrl.Builder httpBuilder = HttpUrl.parse("http://200.126.14.250/api/Station").newBuilder();
        httpBuilder.addQueryParameter("APIKey", Identifiers.APIKey);
        Request request = new Request.Builder().url(httpBuilder.build()).build();
        Identifiers.callSending = okHttpClient.newCall(request);
        try {
            if(!Identifiers.threadRunning){
                return false;
            }
            Response response = Identifiers.callSending.execute();
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
            Log.e("UTILITIES", "ERROR AL PEDIR STATION ID: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch(org.json.JSONException je){
            Log.e("UTILITIES", "ERROR DE JSON AL PEDIR STATION ID: " + je.getMessage());
            je.printStackTrace();
            return false;
        }
    }

}
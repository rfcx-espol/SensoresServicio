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

    public static final String MEASURE_DATABASE_NAME = "measure";
    public static final String MEASURE_TABLE = "measure";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_UNIT = "unit";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_SENSORID = "sensor";


    public static final String IMAGES_TABLE = "image";
    public static final String IMAGE_ID = "id";
    public static final String IMAGE_TIMESTAMP = "timestamp";
    public static final String IMAGE_TYPE = "type";
    public static final String IMAGE_NAME = "name";

    public static final String LIMIT_BY_DEFAULT = "1";
    public static final String LIMIT_BY_DEFAULT_FOR_IMAGES = "1";


    //MÉTODO QUE ENVÍA EL APIKEY Y RECIBE EL ID DE LA ESTACIÓN EN LA BASE DE DATOS
    public static boolean getStationID(OkHttpClient okHttpClient){
        HttpUrl.Builder httpBuilder = HttpUrl.parse("http://200.126.14.250/api/Station").newBuilder();
        httpBuilder.addQueryParameter("APIKey", Identifiers.APIKey);
        Log.d("GET STATION ID", "HTTP: "+String.valueOf(httpBuilder));
        Request request = new Request.Builder().url(httpBuilder.build()).build();
        call = okHttpClient.newCall(request);
        try {
            if(!threadRunning){
                Log.d("GET STATION ID", "THREAD IS RUNNING"+Identifiers.ID_STATION);
                return false;
            }
            Log.d("GET STATION ID", "BEFORE EXECUTE:"+Identifiers.ID_STATION);
            Response response = call.execute();

            if(response.code() == 200){
                String resp = response.body().string();
                Log.d("GET STATION ID", "RESPONSE: "+resp);
                JSONObject obj = new JSONObject(resp);
                if(obj.getString("APIKey").equals(Identifiers.APIKey)){
                    Identifiers.ID_STATION = obj.getString("Id");
                    response.body().close();
                    return true;
                }
                return false;
            } else {
                Log.d("GET STATION ID", "RESP WRONG CODE"+response.code());
                response.body().close();
                return false;
            }
        } catch(Exception e){
            e.printStackTrace();
            Log.d("GET STATION ID", "ERROR: "+e.getMessage());
            return false;
        }
    }
}

package com.example.jorge.blue.activities;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jorge.blue.R;
import com.example.jorge.blue.servicios.SendingService;
import com.example.jorge.blue.servicios.ServiceReceiver;

import static com.example.jorge.blue.utils.Identifiers.delta_time;
import static com.example.jorge.blue.utils.Identifiers.onService;
import static com.example.jorge.blue.utils.Identifiers.onService2;
import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.pendingIntent;
import static com.example.jorge.blue.utils.Identifiers.setAPIKey;

import java.io.IOException;

import java.util.UUID;

public class UserInterfaz extends Activity {

    private static final String TAG = "hooli";
    //1)
    Button IdEncender, IdApagar,IdDesconectar;
    static TextView IdBufferIn;
    //-------------------------------------------

    int c = 20;
    private BluetoothSocket btSocket = null;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    Context thisContext = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interfaz);
        //2)
        //Enlaza los controles con sus respectivas vistas
        IdEncender = (Button) findViewById(R.id.IdEncender);
        IdApagar = (Button) findViewById(R.id.IdApagar);
        IdDesconectar = (Button) findViewById(R.id.IdDesconectar);
        IdBufferIn = (TextView) findViewById(R.id.IdBufferIn);
        //setAPIKey(getApplicationContext());
        //Log.d("APIKEY", Identifiers.APIKey);



        if(!onService) {
            pendingIntent = PendingIntent.getService(getApplicationContext(), 0,
                    new Intent(this, SendingService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), delta_time, pendingIntent);
                Log.d("ALARMA", "ALARMA CREADA");
            }
            //startService(new Intent(thisContext, ServiceReceiver.class));
            onService = true;
        }
//
        if(!onService2) {
            Log.d("Serv", "Servicio invocado");
            startService(new Intent(thisContext, ServiceReceiver.class));
            Toast.makeText(getBaseContext(), "Servicio iniciado", Toast.LENGTH_SHORT).show();
            onService2 = true;

        }
        else
        {
            Toast.makeText(getBaseContext(), "Servicio ya en ejecución", Toast.LENGTH_SHORT).show();
        }


        Log.d("hi", "Servicio Creado");

        // Configuracion onClick listeners para los botones
        // para indicar que se realizara cuando se detecte
        // el evento de Click
        IdEncender.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {

                if(!onService) {
//            PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
//            setPreferencesApplications(getApplicationContext());

                    pendingIntent = PendingIntent.getService(getApplicationContext(), 0,
                            new Intent(thisContext, SendingService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager != null) {
                        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000,
                                delta_time, pendingIntent);
                        Log.d("ALARMA", "ALARMA CREADA");
                    }
                    onService = true;
                }
                if(!onService2) {
                    Log.d("Serv", "Servicio invocado");
                    startService(new Intent(thisContext, ServiceReceiver.class));
                    Toast.makeText(getBaseContext(), "Servicio iniciado", Toast.LENGTH_SHORT).show();
                    onService2 = true;

                }
                else
                {
                    Toast.makeText(getBaseContext(), "Servicio ya en ejecución", Toast.LENGTH_SHORT).show();
                }

//                Calendar cal = Calendar.getInstance();
//                cal.add(Calendar.SECOND, 10);
//                Intent intent = new Intent(thisContext, SendingService.class);
//                PendingIntent pintent = PendingIntent.getService(thisContext, 0, intent,
//                        0);
//                AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//                alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
//                        10 * 1000, pintent);
//
//
//                Log.d("hi", "Servicio Creado");

            }
        });

        IdApagar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(thisContext, ServiceReceiver.class));
                stopService(new Intent(thisContext, SendingService.class));
                Log.d("hi", "Servicio Apagado");
                onService = false;
                onService2 = false;
                //c=0;
            }
        });

        IdDesconectar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(thisContext, ServiceReceiver.class));
                stopService(new Intent(thisContext, SendingService.class));
                if (btSocket!=null)
                {
                    try {btSocket.close();}
                    catch (IOException e)
                    { Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();}
                }
                onService=false;
                onService2=false;
                finish();
                c=0;

            }
        });
    }



    @Override
    public void onResume()
    {
        super.onResume();

    }

    @Override
    public void onPause()
    {
        super.onPause();
    }





}
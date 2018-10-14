package com.example.jorge.blue.activities;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.example.jorge.blue.R;
import com.example.jorge.blue.servicios.SendingService;
import com.example.jorge.blue.servicios.ServiceReceiver;
import static com.example.jorge.blue.utils.Identifiers.onSendingService;
import static com.example.jorge.blue.utils.Identifiers.onServiceReceiver;
import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.pendingIntentSending;
import static com.example.jorge.blue.utils.Identifiers.pendingIntentReceiver;
import static com.example.jorge.blue.utils.Identifiers.callSending;
import static com.example.jorge.blue.utils.Identifiers.callReceiver;
import java.util.UUID;

public class UserInterfaz extends AppCompatActivity {
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "USER INTERFAZ";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interfaz);

        if(!onSendingService && !onServiceReceiver) {
            createAlarms();
        }
        setContentView(R.layout.activity_user_interfaz);

        /*idDesconectar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(onSendingService && onServiceReceiver) {
                    alarmManager.cancel(pendingIntentReceiver);
                    alarmManager.cancel(pendingIntentSending);
                    stopService(new Intent(getApplicationContext(), ServiceReceiver.class));
                    stopService(new Intent(getApplicationContext(), SendingService.class));
                    if (btSocket != null) {
                        try {
                            btSocket.close();
                        } catch (IOException e) {
                            Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                    onSendingService = false;
                    onServiceReceiver = false;
                    Toast.makeText(getApplicationContext(), "SERVICIOS DETENIDOS", Toast.LENGTH_LONG).show();
                    //finish();
                    c = 0;
                } else {
                    Toast.makeText(getApplicationContext(), "LOS SERVICIOS NO SE ESTÁN EJECUTANDO",
                            Toast.LENGTH_LONG).show();
                }
            }
        });*/
    }

    //CREAR LAS ALARMAS PARA ENVIAR Y RECIBIR DATOS
    private void createAlarms() {
        if(!onServiceReceiver) {
            pendingIntentReceiver = PendingIntent.getService(getApplicationContext(), 0,
                    new Intent(this, ServiceReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                        pendingIntentReceiver);
            }
            onServiceReceiver = true;
        }
        /*if(!onSendingService) {
            pendingIntentSending = PendingIntent.getService(getApplicationContext(), 0,
                    new Intent(this, SendingService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                        pendingIntentSending);
            }
            onSendingService = true;
        }*/
    }

    //REINICIAR LAS ALARMAS
    public void reboot(){
        Intent intentSendingData = new Intent(getApplicationContext(), SendingService.class);
        Intent intentReceivingData = new Intent(getApplicationContext(), ServiceReceiver.class);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SendingService.class.getName().equals(service.service.getClassName())) {
                alarmManager.cancel(pendingIntentSending);
                if(callSending != null)
                    callSending.cancel();
                //SendingService.thread.interrupt();
                stopService(intentSendingData);
                onSendingService = false;
            }
            if (ServiceReceiver.class.getName().equals(service.service.getClassName())) {
                alarmManager.cancel(pendingIntentReceiver);
                if(callReceiver != null)
                    callReceiver.cancel();
                stopService(intentReceivingData);
                onServiceReceiver = false;
            }
        }
        createAlarms();
        Toast.makeText(getApplicationContext(), "SERVICIOS REINICIADOS CORRECTAMENTE", Toast.LENGTH_LONG).show();
        Log.i(TAG, "SERVICIOS REINICIADOS CORRECTAMENTE");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reboot:
                reboot();
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
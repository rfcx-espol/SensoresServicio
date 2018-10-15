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
import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.servicios.SendingService;
import com.example.jorge.blue.servicios.ServiceReceiver;
import com.example.jorge.blue.utils.Identifiers;

import java.io.IOException;

import static com.example.jorge.blue.utils.Identifiers.onSendingService;
import static com.example.jorge.blue.utils.Identifiers.onServiceReceiver;
import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.pendingIntentSending;
import static com.example.jorge.blue.utils.Identifiers.pendingIntentReceiver;
import static com.example.jorge.blue.utils.Identifiers.callSending;
import static com.example.jorge.blue.utils.Identifiers.callReceiver;

public class UserInterfaz extends AppCompatActivity {
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

    }

    //CREAR LAS ALARMAS PARA ENVIAR Y RECIBIR DATOS
    private void createAlarms() {
        Identifiers.connection = new ConexionSQLiteHelper(getApplicationContext(), "medicion", null, 1);
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
    public void reboot() {
        Intent intentSendingData = new Intent(getApplicationContext(), SendingService.class);
        Intent intentReceivingData = new Intent(getApplicationContext(), ServiceReceiver.class);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SendingService.class.getName().equals(service.service.getClassName())) {
                alarmManager.cancel(pendingIntentSending);
                if(callSending != null)
                    callSending.cancel();
                SendingService.thread.interrupt();
                stopService(intentSendingData);
                onSendingService = false;
            }
            if (ServiceReceiver.class.getName().equals(service.service.getClassName())) {
                alarmManager.cancel(pendingIntentReceiver);
                if(callReceiver != null)
                    callReceiver.cancel();
                try {
                    ServiceReceiver.btSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "ERROR AL DESCONECTAR EL SOCKET: " + e.getMessage());
                    e.printStackTrace();
                }
                stopService(intentReceivingData);
                onServiceReceiver = false;
            }
        }
        createAlarms();
        Toast.makeText(getApplicationContext(), "SERVICIOS REINICIADOS CORRECTAMENTE", Toast.LENGTH_LONG).show();
        Log.i(TAG, "SERVICIOS REINICIADOS CORRECTAMENTE");
    }

    public void disconnect() {
        if(onSendingService || onServiceReceiver) {
            if(callSending != null)
                callSending.cancel();
            if(callReceiver != null)
                callReceiver.cancel();
            alarmManager.cancel(pendingIntentReceiver);
            alarmManager.cancel(pendingIntentSending);
            stopService(new Intent(getApplicationContext(), ServiceReceiver.class));
            stopService(new Intent(getApplicationContext(), SendingService.class));
            if(ServiceReceiver.btSocket != null) {
                Log.e(TAG, "SOCKET NO ES NULL");
                try {
                    ServiceReceiver.btSocket.close();
                } catch (IOException e) {
                    Toast.makeText(getBaseContext(), "ERROR AL DESCONECTAR", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "ERROR AL DESCONECTAR EL SOCKET: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            onSendingService = false;
            onServiceReceiver = false;
            Toast.makeText(getApplicationContext(), "SERVICIOS DETENIDOS", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "LOS SERVICIOS NO SE ESTÁN EJECUTANDO",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reboot:
                reboot();
                break;
            case R.id.disconnect:
                disconnect();
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
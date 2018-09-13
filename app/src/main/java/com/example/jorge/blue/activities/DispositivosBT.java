package com.example.jorge.blue.activities;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import static com.example.jorge.blue.utils.Identifiers.onService;
import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.pendingIntent;


import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.R;
import com.example.jorge.blue.servicios.ServiceReceiver;


import java.util.List;
import java.util.Set;

public class DispositivosBT extends AppCompatActivity {

    //1)
    // Depuración de LOGCAT
    private static final String TAG = "DispositivosBT"; //<-<- PARTE A MODIFICAR >->->
    // Declaracion de ListView
    ListView IdLista;
    // String que se enviara a la actividad principal, mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Declaracion de campos
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter mPairedDevicesArrayAdapter;
    Context thisContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "mediciones", null, 1);




//        // Inicializa la array que contendra la lista de los dispositivos bluetooth vinculados
//        mPairedDevicesArrayAdapter = new ArrayAdapter(this, R.layout.nombres_dispositivos);//<-<- PARTE A MODIFICAR >->->
//        // Presenta los disposisitivos vinculados en el ListView
//        IdLista = (ListView) findViewById(R.id.IdLista);
//        IdLista.setAdapter(mPairedDevicesArrayAdapter);
//        IdLista.setOnItemClickListener(mDeviceClickListener);
//        // Obtiene el adaptador local Bluetooth adapter
//        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        //------------------- EN CASO DE ERROR -------------------------------------
//        //SI OBTIENES UN ERROR EN LA LINEA (BluetoothDevice device : pairedDevices)
//        //CAMBIA LA SIGUIENTE LINEA POR
//        //Set <BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
//        //------------------------------------------------------------------------------
//
//        // Obtiene un conjunto de dispositivos actualmente emparejados y agregua a 'pairedDevices'
//        Set <BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
//
//        // Adiciona un dispositivos previo emparejado al array
//        if (pairedDevices.size() > 0)
//        {
//            for (BluetoothDevice device : pairedDevices) { //EN CASO DE ERROR LEER LA ANTERIOR EXPLICACION
//                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//            }
//        }
//        String info = IdLista.getAdapter().toString();
//        Log.d("adap", info);
//        String address = info.substring(info.length() - 17);
//
//        //INICIAR EL SERVICIO
//        if(!onService) {
//
//            pendingIntent = PendingIntent.getService(getApplicationContext(), 0,
//                    new Intent(thisContext, ServiceReceiver.class).putExtra(EXTRA_DEVICE_ADDRESS, address), PendingIntent.FLAG_UPDATE_CURRENT);
//            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
////            if (alarmManager != null) {
////                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000,
////                        recordingAudioInterval + audioDuration , pendingIntent);
////                Log.d("ALARMA", "ALARMA CREADA");
////            }
//            onService = true;
//        }

        setContentView(R.layout.activity_dispositivos_bt);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //---------------------------------
        VerificarEstadoBT();

        // Inicializa la array que contendra la lista de los dispositivos bluetooth vinculados
        mPairedDevicesArrayAdapter = new ArrayAdapter(this, R.layout.nombres_dispositivos);//<-<- PARTE A MODIFICAR >->->
        // Presenta los disposisitivos vinculados en el ListView
        IdLista = (ListView) findViewById(R.id.IdLista);
        IdLista.setAdapter(mPairedDevicesArrayAdapter);
        IdLista.setOnItemClickListener(mDeviceClickListener);
        // Obtiene el adaptador local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        //------------------- EN CASO DE ERROR -------------------------------------
        //SI OBTIENES UN ERROR EN LA LINEA (BluetoothDevice device : pairedDevices)
        //CAMBIA LA SIGUIENTE LINEA POR
        //Set <BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        //------------------------------------------------------------------------------

        // Obtiene un conjunto de dispositivos actualmente emparejados y agregua a 'pairedDevices'
        Set <BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Adiciona un dispositivos previo emparejado al array
        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices) { //EN CASO DE ERROR LEER LA ANTERIOR EXPLICACION
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        String info = IdLista.getAdapter().toString();
        Log.d("adap", info);
        String address = info.substring(info.length() - 17);

//        //INICIAR EL SERVICIO
//        if(!onService) {
//
//            pendingIntent = PendingIntent.getService(getApplicationContext(), 0,
//                    new Intent(thisContext, ServiceReceiver.class).putExtra(EXTRA_DEVICE_ADDRESS, address), PendingIntent.FLAG_UPDATE_CURRENT);
//            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
////            if (alarmManager != null) {
////                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000,
////                        recordingAudioInterval + audioDuration , pendingIntent);
////                Log.d("ALARMA", "ALARMA CREADA");
////            }
//            onService = true;
//        }
    }

    // Configura un (on-click) para la lista
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {

            // Obtener la dirección MAC del dispositivo, que son los últimos 17 caracteres en la vista
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Realiza un intent para iniciar la siguiente actividad
            // mientras toma un EXTRA_DEVICE_ADDRESS que es la dirección MAC.
            Intent i = new Intent(DispositivosBT.this, UserInterfaz.class);//<-<- PARTE A MODIFICAR >->->
            //i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            Log.d("click", "CREADA,"+address);
            //INICIAR EL SERVICIO
//            if(!onService) {
//
////                pendingIntent = PendingIntent.getService(getApplicationContext(), 0,
////                        new Intent(thisContext, ServiceReceiver.class).putExtra(EXTRA_DEVICE_ADDRESS, address), PendingIntent.FLAG_UPDATE_CURRENT);
////                alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//                startService(new Intent(thisContext, ServiceReceiver.class).putExtra(EXTRA_DEVICE_ADDRESS, address));
////            if (alarmManager != null) {
////                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000,
////                        recordingAudioInterval + audioDuration , pendingIntent);
//                Log.d("ALARMA", "ALARMA CREADA");
////            }
//                onService = true;
//            }

            startActivity(i);


        }
    };

    private void VerificarEstadoBT() {
        // Comprueba que el dispositivo tiene Bluetooth y que está encendido.
        mBtAdapter= BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth Activado...");

            } else {
                //Solicita al usuario que active Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);

            }
        }
    }

    private boolean isExecuting() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++) {
            if(procInfos.get(i).processName.equals("com.example.jorge.servicereceiver")) {
                return true; //EL SERVICIO ESTÁ ACTIVO
            }
        }
        return false; //EL SERVICIO ESTÁ INACTIVO
    }

}
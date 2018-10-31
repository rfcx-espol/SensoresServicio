package com.example.jorge.blue.servicios;

/**
 * Created by JORGE on 31/5/18.
 */
import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;


import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


import com.example.jorge.blue.R;
import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.utils.Utilities;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static com.example.jorge.blue.utils.Identifiers.BT_address;

public class ServiceReceiver extends Service{

    public static PowerManager.WakeLock wakeLock;
    public static String TAG = "ServiceReceiver";

    private static final String ACTION_USB_PERMISSION = "com.example.jorge.blue.USB_PERMISSION";

    Context thisContext = this;
    int c = 20;
    final int handlerState = 0;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    private ArrayAdapter mPairedDevicesArrayAdapter;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "medicion", null, 1);



    // Has the accessory been opened? find out with this variable
    private boolean isOpen = false;
    private boolean mPermissionRequestPending;
    PendingIntent mPermissionIntent;
    Handler usbHandler;
    // The file bits
    private ParcelFileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    // Attaches the file streams to their pointers
    private void openAccessory(UsbAccessory accessory) {

        mAccessory = accessory;
        mFileDescriptor = mManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Thread thread = new ConnectedThread(mInputStream, mOutputStream);
            thread.start();
            Log.d(TAG, "accessory opened");
        }
        isOpen = true;
    }


    public void closeAccessory() {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
        isOpen = false;

    }

    // A receiver for events on the UsbManager, when permission is given or when the cable is pulled
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = UsbManager.getAccessory(intent);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {

                    }

                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = UsbManager.getAccessory(intent);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }

        }
    };



    // An instance of accessory and manager
    private UsbAccessory mAccessory;
    private UsbManager mManager;
    //-------------------------------------------

    @Override
    public void onCreate(){
        Log.d("USB", "USB Service is running");

        //MANTENER ENCENDIDO EL CPU DEL CELULAR AL APAGAR LA PANTALLA
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();




    }



    @Override
    public int onStartCommand(Intent intent, int flag, int idProcess)
    {


       mManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
          IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
          this.registerReceiver(mUsbReceiver, filter);
          setup(this.getApplicationContext());
//
//        usbHandler = new Handler() {
//            public void handleMessage(android.os.Message msg) {
//                Log.d("BT", "Handler creado");
//                if (msg.what == handlerState) {
//                    String readMessage = (String) msg.obj;
//                    DataStringIN.append(readMessage);
//
//                    int endOfLineIndex = DataStringIN.indexOf("#");
//
//                    if (endOfLineIndex > 0) {
//                        String medicion = DataStringIN.substring(0, endOfLineIndex);
//                        String[] parts = medicion.split(",");
//                        String sensorId = parts[0];
//                        String type = parts[1];
//                        String value = parts[2];
//                        String unit = parts[3];
//                        String location = parts[4];
//                        Long tsLong = System.currentTimeMillis()/1000;
//                        String ts = tsLong.toString();
//                        registrarMedicion(ts, type, value, unit, location, sensorId);
//                        DataStringIN.delete(0, DataStringIN.length());
//                    }
//                }
//            }
//        };

        return START_STICKY;
    }


    // Sets up all the requests for permission and attaches the USB accessory if permission is already granted
    public void setup(Context context)
    {

        Log.d("MESSAGE", "SETUP");
        UsbAccessory[] accessoryList = mManager.getAccessoryList();

        UsbAccessory accessory = (accessoryList == null ? null : accessoryList[0]);

        if (accessory != null) {

            if (mManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mManager.requestPermission(accessory,
                                mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        }
        else {
            Log.d(TAG, "mAccessory is null");
        }

    }




    @Override
    public void onDestroy()
    {
        closeAccessory();
        wakeLock.release();
        unregisterReceiver(mUsbReceiver);

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }





    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(InputStream mmInStream, OutputStream mmOutStream)
        {
            this.mmInStream = mmInStream;
            this.mmOutStream = mmOutStream;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    usbHandler.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //Envio de trama, esto sirve para enviar desde el celular al BT, por ahora no se la usa
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
            }
        }

    }


    public void registrarMedicion(String ts, String type, String value, String unit, String location, String id)
    {
        SQLiteDatabase db = conn.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Utilities.CAMPO_TIMESTAMP, ts);
        values.put(Utilities.CAMPO_TYPE, type);
        values.put(Utilities.CAMPO_VALUE, value);
        values.put(Utilities.CAMPO_UNIT, unit);
        values.put(Utilities.CAMPO_LOCATION, location);
        values.put(Utilities.CAMPO_SENSORID, id);

        long result = db.insert(Utilities.TABLA_MEDICION, Utilities.CAMPO_SENSORID, values);
        Log.d("DB", "ingresado el timestamp, dato, unit:" + ts +","+ value + "," + unit);
        db.close();

    }

}

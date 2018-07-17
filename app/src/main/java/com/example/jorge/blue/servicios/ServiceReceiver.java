package com.example.jorge.blue.servicios;

/**
 * Created by JORGE on 31/5/18.
 */

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.utils.Utilities;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ServiceReceiver extends Service{

    public static PowerManager.WakeLock wakeLock;

    Context thisContext = this;
    Handler bluetoothIn;
    int c = 20;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = "98:D3:36:00:97:BA";
    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "medicion", null, 1);

    //-------------------------------------------

    @Override
    public void onCreate(){
        Log.d("hi", "Servicio Creado");

        address = "98:D3:36:00:97:BA";


        //MANTENER ENCENDIDO EL CPU DEL CELULAR AL APAGAR LA PANTALLA
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();



    }

    @Override
    public int onStartCommand(Intent intent, int flag, int idProcess)
    {
        //Intent intent = getIntent();
        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
        //address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);//<-<- PARTE A MODIFICAR >->->
        //Setea la direccion MAC
        //Log.d("BT", "address obtenida");

        //Setea la direccion MAC

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Log.d("hi", "Handler creado");
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);

                    int endOfLineIndex = DataStringIN.indexOf("#");

                    if (endOfLineIndex > 0) {
                        String medicion = DataStringIN.substring(0, endOfLineIndex);
                        String[] parts = medicion.split(",");
                        String sensorId = parts[0];
                        String type = parts[1];
                        String value = parts[2];
                        String unit = parts[3];
                        String location = parts[4];
                        Long tsLong = System.currentTimeMillis()/1000;
                        String ts = tsLong.toString();


                        //UserInterfaz.IdBufferIn.setText("Dato: " + medicion);//<-<- PARTE A MODIFICAR >->->
                        //SaveData(ts+".txt", medicion);
                        //Toast.makeText(thisContext,"Data Guardada",Toast.LENGTH_SHORT).show();
                        //Log.d("hi", "Data Guardada");
                        registrarMedicion(ts, type, value, unit, location, sensorId);
                        //String[] datofinal = consultarMedicion();
                        Log.d("Dato", ts+", "+type+", "+value+", "+unit+", "+location+", "+sensorId);
                        //UserInterfaz.IdBufferIn.setText("Dato: " + datofinal[0] +", "+datofinal[1]+", "+datofinal[2]);//<-<- PARTE A MODIFICAR >->->
                        DataStringIN.delete(0, DataStringIN.length());
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter



        BluetoothDevice device = btAdapter.getRemoteDevice(address);


        while (true) {
            try {
                btSocket = createBluetoothSocket(device);
                Log.d("BT", "Socket creado");
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
            }
            // Establece la conexión con el socket Bluetooth.
            try {
                btSocket.connect();
                Log.d("BT", "Socket conectado");
                break;


            } catch (IOException e) {
                try {
                    btSocket.close();
                    Log.d("BT", "Socket cerrado");
                } catch (IOException e2) {
                }
            }
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();





        //btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
//        Tiempo a = new Tiempo();
//        Log.d("hi", "por ejecutar el hilo");
//        c=20;
//        a.execute();
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        //Log.d("entro aqui", "si");
        c=50;
        if (btSocket!=null)
        {
            try {btSocket.close();}
            catch (IOException e)
            { Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();;}
        }
        wakeLock.release();

    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }



    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
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
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //Envio de trama
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
        //ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "mediciones", null, 1);
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

    public String[] consultarMedicion()
    {
        SQLiteDatabase db = conn.getReadableDatabase();
        String[] parametros = {};
        String[] campos = {Utilities.CAMPO_VALUE, Utilities.CAMPO_TYPE, Utilities.CAMPO_UNIT};
        String[] salida =  new String[3];

        try {
            Cursor cursor = db.query(Utilities.TABLA_MEDICION, campos,  null, null, null, null, null);
            cursor.moveToLast();
            salida[0] = cursor.getString(0);
            salida[1] = cursor.getString(1);
            salida[2] = cursor.getString(2);
            cursor.close();

        }catch (Exception e)
        {
            Log.d("DB", "no se pudo cargar datos desde la base");
        }
        conn.close();
        return salida;
    }
}

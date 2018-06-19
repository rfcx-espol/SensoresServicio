package com.example.jorge.blue.activities;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.R;
import com.example.jorge.blue.servicios.ServiceReceiver;
import com.example.jorge.blue.utils.Utilities;
import static com.example.jorge.blue.utils.Identifiers.onService;
import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.pendingIntent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class UserInterfaz extends AppCompatActivity {

    private static final String TAG = "hooli";
    //1)
    Button IdEncender, IdApagar,IdDesconectar;
    static TextView IdBufferIn;
    public static boolean onService = false;
    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    int c = 20;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    //private static String address = null;
    //------------------------null-------------------
    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "mediciones", null, 1);

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

        if(!onService) {
            Log.d("Serv", "Servicio invocado");
            startService(new Intent(thisContext, ServiceReceiver.class));
            Toast.makeText(getBaseContext(), "Servicio iniciado", Toast.LENGTH_SHORT).show();
            onService = true;

        }
        else
        {
            Toast.makeText(getBaseContext(), "Servicio ya en ejecución", Toast.LENGTH_SHORT).show();
        }

//        bluetoothIn = new Handler() {
//            public void handleMessage(android.os.Message msg) {
//                Log.d("hi", "Handler creado");
//                if (msg.what == handlerState) {
//                    String readMessage = (String) msg.obj;
//                    DataStringIN.append(readMessage);
//
//                    int endOfLineIndex = DataStringIN.indexOf("#");
//
//                    if (endOfLineIndex > 0) {
//                        String medicion = DataStringIN.substring(0, endOfLineIndex);
//                        String[] parts = medicion.split(",");
//                        String type = parts[0];
//                        String value = parts[1];
//                        String unit = parts[2];
//                        String location = parts[3];
//                        Long tsLong = System.currentTimeMillis()/1000;
//                        String ts = tsLong.toString();
//
//
//                        //UserInterfaz.IdBufferIn.setText("Dato: " + medicion);//<-<- PARTE A MODIFICAR >->->
//                        //SaveData(ts+".txt", medicion);
//                        //Toast.makeText(thisContext,"Data Guardada",Toast.LENGTH_SHORT).show();
//                        //Log.d("hi", "Data Guardada");
//                        registrarMedicion(ts, type, value, unit, location);
//                        String[] datofinal = consultarMedicion();
//                        UserInterfaz.IdBufferIn.setText("Dato: " + datofinal[0] +", "+datofinal[1]+", "+datofinal[2]);//<-<- PARTE A MODIFICAR >->->
//                        DataStringIN.delete(0, DataStringIN.length());
//                    }
//                }
//            }
//        };
        Log.d("hi", "Servicio Creado");

        //btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter

        // Configuracion onClick listeners para los botones
        // para indicar que se realizara cuando se detecte
        // el evento de Click
        IdEncender.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if(!onService) {
                    Log.d("Serv", "Servicio invocado");
                    startService(new Intent(thisContext, ServiceReceiver.class));
                    Toast.makeText(getBaseContext(), "Servicio iniciado", Toast.LENGTH_SHORT).show();
                    onService = true;

                }
                else
                {
                    Toast.makeText(getBaseContext(), "Servicio ya en ejecución", Toast.LENGTH_SHORT).show();
                }

            }
        });

        IdApagar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //MyConexionBT.write("CHAO#");
                //Log.d(TAG, "000000000");
                stopService(new Intent(thisContext, ServiceReceiver.class));
                Log.d("hi", "Servicio Apagado");
                onService = false;
                //c=0;

            }
        });

        IdDesconectar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //stopService(new Intent(thisContext, ServiceReceiver.class));
                if (btSocket!=null)
                {
                    try {btSocket.close();}
                    catch (IOException e)
                    { Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();}
                }
                onService=false;
                finish();
                c=0;

            }
        });
    }



    @Override
    public void onResume()
    {
        super.onResume();
//        //Consigue la direccion MAC desde DeviceListActivity via intent
//        Intent intent = getIntent();
//        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
//        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);//<-<- PARTE A MODIFICAR >->->
//        //Setea la direccion MAC
//        BluetoothDevice device = btAdapter.getRemoteDevice(address);
//
//        try
//        {
//            btSocket = createBluetoothSocket(device);
//        } catch (IOException e) {
//            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
//        }
//        // Establece la conexión con el socket Bluetooth.
//        try
//        {
//            btSocket.connect();
//        } catch (IOException e) {
//            try {
//                btSocket.close();
//            } catch (IOException e2) {}
//        }
//        MyConexionBT = new ConnectedThread(btSocket);
//        MyConexionBT.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
//        try
//        { // Cuando se sale de la aplicación esta parte permite
//            // que no se deje abierto el socket
//            btSocket.close();
//        } catch (IOException e2) {}
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    private void SaveData(String filename, String body)
    {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Mediciones");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, filename);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(body+"\n");
            writer.flush();
            writer.close();
            //Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

//    public void ejecutar(){
//        Tiempo a = new Tiempo();
//        if(c!=0) {
//            a.execute();
//        }
//        c++;
//        Log.d("hi", "hilo ejecutado");
//
//    }
//
//    public void hilo() {
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public class Tiempo extends AsyncTask<Void,Integer,Boolean> {
//        @Override
//        protected Boolean doInBackground(Void... voids) {
//            for(int i=0;i<5;i++){
//                hilo();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean aBoolean) {
//            super.onPostExecute(aBoolean);
//            ejecutar();
//            MyConexionBT.write("temperature,"+c+",celsius,device#");
//            //Toast.makeText(MainActivity.this,"hola",Toast.LENGTH_SHORT).show();
//
//        }
//    }

    public void registrarMedicion(String ts, String type, String value, String unit, String location)
    {
        //ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, "mediciones", null, 1);
        SQLiteDatabase db = conn.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Utilities.CAMPO_TIMESTAMP, ts);
        values.put(Utilities.CAMPO_TYPE, type);
        values.put(Utilities.CAMPO_VALUE, value);
        values.put(Utilities.CAMPO_UNIT, unit);
        values.put(Utilities.CAMPO_LOCATION, location);

        long result = db.insert(Utilities.TABLA_MEDICION, Utilities.CAMPO_VALUE, values);
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
package com.example.jorge.blue.servicios;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.utils.Identifiers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class ServiceReceiver extends Service{
    private final IBinder mBinder = new LocalBinder();
    private final int handlerState = 0;
    private static final String TAG = "SERVICE RECEIVER";
    private BluetoothAdapter btAdapter;
    public static BluetoothSocket btSocket;
    private StringBuilder DataStringIN = new StringBuilder();
    //public ConnectThread connectThread;
    public ConnectedThread connectedThread;
    //public AcceptThread acceptThread;
    public static PowerManager.WakeLock wakeLock;
    public static Handler bluetoothIn;

    public class LocalBinder extends Binder {
        public ServiceReceiver getService() {
            return ServiceReceiver.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate(){
        Identifiers.setAPIKey(getApplicationContext());
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Identifiers.connection = new ConexionSQLiteHelper(this, "medicion", null, 1);
        if(btAdapter == null) {
            Log.i(TAG, "ESTE DISPOSITIVO NO SOPORTA BLUETOOTH");
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Identifiers.BT_address = device.getAddress();
                Log.i(TAG, "ESTÁ EMPAREJADO");
            }
        }

        //MANTENER ENCENDIDO EL CPU DEL CELULAR AL APAGAR LA PANTALLA
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
    }

    @SuppressLint("HandlerLeak")
    @Override
    public int onStartCommand(Intent intent, int flag, int idProcess) {
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
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
                        registrarMedicion(ts, type, value, unit, location, sensorId);
                        DataStringIN.delete(0, DataStringIN.length());
                    }
                }
            }
        };

        while(true) {
            if(!btAdapter.isEnabled()) {
                Log.e(TAG, "EL BLUETOOTH NO ESTÁ ENCENDIDO. ESPERANDO");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ERROR EN LA ESPERA DEL ENCENDIDO DEL BLUETOOTH");
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }

        BluetoothDevice btDevice = btAdapter.getRemoteDevice(Identifiers.BT_address);

        while(true) {
            try {
                final Method m = btDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                btSocket = (BluetoothSocket) m.invoke(btDevice, Identifiers.BTMODULEUUID);
                //btSocket = btDevice.createRfcommSocketToServiceRecord(Identifiers.BTMODULEUUID);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            // Establece la conexión con el socket Bluetooth.
            try {
                btAdapter.cancelDiscovery();
                btSocket.connect();
                break;
            } catch (IOException e) {
                Log.e(TAG, "ERROR DE CONEXIÓN DEL SOCKET: " + e.getMessage());
                e.printStackTrace();
                try {
                    btSocket.close();
                    Log.i(TAG, "SOCKET CERRADO EXITOSAMENTE");
                } catch (IOException e2) {
                    Log.e(TAG, "ERROR AL CERRAR EL SOCKET: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        connectedThread = new ConnectedThread(btSocket);
        connectedThread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ERROR AL CERRAR EL SOCKET: " + e.getMessage());
                e.printStackTrace();
            }
        }
        wakeLock.release();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("blue", Identifiers.BTMODULEUUID);
            } catch (IOException e) {
                Log.e(TAG, "ERROR: " + e.getMessage());
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            btSocket = null;

            // Keep listening until exception occurs or a socket is returned
            while (true) {

                try {
                    Log.e(TAG, "ESPERANDO");
                    btSocket = mmServerSocket.accept();

                } catch (IOException e) {
                    Log.e(TAG, "ERROR AL ACEPTAR EL SOCKET DESDE EL SERVIDOR: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
                // If a connection was accepted
                if (btSocket != null) {
                    // Do work to manage the connection (in a separate thread)
                    Log.e(TAG, "SE CONECTÓ");
                    connectedThread = new ConnectedThread(btSocket);
                    connectedThread.start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "ERROR AL CERRAR EL SOCKET DESDE EL SERVIDOR: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ERROR AL CANCELAR EL SOCKET DESDE EL SERVIDOR: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice btDevice) {
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                btSocket = btDevice.createRfcommSocketToServiceRecord(Identifiers.BTMODULEUUID);
            } catch (IOException e) {
                Log.e(TAG, "ERROR DE CREACIÓN DEL SOCKET: " + e.getMessage());
                e.printStackTrace();
            }
            mmSocket = btSocket;
        }

        public void run() {

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                btAdapter.cancelDiscovery();
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.e(TAG, "ERROR DE CONEXIÓN: " + connectException.getMessage());
                connectException.printStackTrace();
                try {
                    mmSocket.close();
                    Log.i(TAG, "SOCKET CERRADO EXITOSAMENTE");
                } catch (IOException closeException) {
                    Log.e(TAG, "ERROR AL CERRAR EL SOCKET: " + closeException.getMessage());
                    closeException.printStackTrace();
                }
            }

            connectedThread = new ConnectedThread(btSocket);
            connectedThread.start();
        }

        //Will cancel an in-progress connection, and close the socket
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ERROR AL CERRAR EL SOCKET: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            Log.i(TAG, "HILO CONECTADO");
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ERROR AL OBTENER LOS STREAMS: " + e.getMessage());
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;
            Log.i(TAG, "RUN");
            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "ERROR AL OBTENER EL MENSAJE DEL BLUETOOTH: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        }

        //Envio de trama, esto sirve para enviar desde el celular al BT, por ahora no se la usa
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "NO SE PUEDE ENVIAR DATOS", Toast.LENGTH_LONG).show();
                Log.e(TAG, "ERROR AL ENVIAR LOS DATOS: " + e.getMessage());
                e.printStackTrace();
            }
        }

    }

    //GUARDA LOS VALORES RECIBIDOS EN LA BASE DE DATOS DEL DISPOSITIVO
    public void registrarMedicion(String ts, String type, String value, String unit, String location, String id) {
        SQLiteDatabase db = Identifiers.connection.getWritableDatabase();
        /*while(true) {
            Log.e(TAG, "LA BASE ESTÁ ABIERTA: " + db.isOpen());
            if(!db.isOpen()) {*/
                ContentValues values = new ContentValues();
                values.put(Identifiers.CAMPO_TIMESTAMP, ts);
                values.put(Identifiers.CAMPO_TYPE, type);
                values.put(Identifiers.CAMPO_VALUE, value);
                values.put(Identifiers.CAMPO_UNIT, unit);
                values.put(Identifiers.CAMPO_LOCATION, location);
                values.put(Identifiers.CAMPO_SENSORID, id);
                long result = db.insert(Identifiers.TABLA_MEDICION, Identifiers.CAMPO_SENSORID, values);
                Log.i(TAG, "DATOS: TIMESTAMP: " + ts + ", VALOR: " + value + ", UNIDAD: " + unit);
                Log.i(TAG, "RESULTADO: " + result);
                db.close();
                Identifiers.connection.close();
        /*        break;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Log.e(TAG, "ERROR AL GUARDAR, LA BASE ESTÁ OCUPADA");
                e.printStackTrace();
            }
        }*/
    }

}
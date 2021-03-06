package com.example.jorge.blue.servicios;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;


import com.example.jorge.blue.activities.UsbEventReceiverActivity;
import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.utils.Utilities;
import com.facebook.infer.annotation.IntegritySink;
import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.delta_time;
import static com.example.jorge.blue.utils.Identifiers.onService;
import static com.example.jorge.blue.utils.Identifiers.pendingIntent;

public class ServiceReceiver extends Service {

    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    public static final int SYNC_READ = 3;
    public static final int SYNC_PHOTO = 4;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 2000000; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;

    private IBinder binder = new UsbBinder();

    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    //private StringBuilder dataBuffer = new StringBuilder();
    private String TAG = "USB-RECEIVER";
    private int MAX_VALUE = 1503600;
    byte[] imageBuffer = new byte[MAX_VALUE];  // 15KB reserved
    private FileOutputStream ImageOutStream;
    private StringBuffer dataBuffer = new StringBuffer();
    private static int fileIndex = 0;
    private static int finalIndex = -1;
    private boolean validImage = false;
    private boolean processingImage = false;

    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, Utilities.MEASURE_DATABASE_NAME, null, 1);

    private boolean serialPortConnected;
    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */
    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                Log.d(TAG, "Length:" + arg0.length);
                String data = new String(arg0, "UTF-8");
                if (mHandler != null)
                    mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data).sendToTarget();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
            if (mHandler != null)
                mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
        }
    };

    /*
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if (mHandler != null)
                mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
        }
    };
    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    connection = usbManager.openDevice(device);
                    new ConnectionThread().start();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                if (serialPortConnected) {
                    serialPort.syncClose();
                }
                serialPortConnected = false;
            }
        }
    };

    private final BroadcastReceiver attachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(UsbEventReceiverActivity.ACTION_USB_DEVICE_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
                connection = usbManager.openDevice(device);
                new ConnectionThread().start();
            }
        }
    };

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        this.context = this;
        serialPortConnected = false;
        ServiceReceiver.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
        Intent i = new Intent(ACTION_USB_PERMISSION_GRANTED); // Broadcast connection to UserInterfaz
        this.sendBroadcast(i);

    }

    /* MUST READ about services
     * http://developer.android.com/guide/components/services.html
     * http://developer.android.com/guide/components/bound-services.html
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!onService) {
            pendingIntent = PendingIntent.getService(context, 0,
                    new Intent(context, SendingService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                        delta_time, pendingIntent);
                Log.d("ALARMA", "ALARMA CREADA DESPUÉS DE REINICIAR EL DISPOSITIVO");
            }
            onService = true;
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceReceiver.SERVICE_CONNECTED = false;
    }

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void write(byte[] data) {
        if (serialPort != null)
            serialPort.syncWrite(data, 0);
    }

    /*
     * This function will be called from MainActivity to change baud rate
     */

    public void changeBaudRate(int baudRate) {
        //if(serialPort != null)
        //     serialPort.setBaudRate(baudRate);
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbEventReceiverActivity.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(attachReceiver, filter);
    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    public class UsbBinder extends Binder {
        public ServiceReceiver getService() {
            return ServiceReceiver.this;
        }
    }

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.syncOpen()) {
                    serialPortConnected = true;
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);
                    serialPort.getCTS(ctsCallback);
                    serialPort.getDSR(dsrCallback);

                    new ReadThread().start();

                    //
                    // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
                    // to be uploaded or not
                    //Thread.sleep(2000); // sleep some. YMMV with different chips.

                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        context.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        context.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }

    public Bitmap StringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    public boolean checkSDCard() {
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean saveImagetoSD() {
        Log.i(TAG, "saveImagetoSD ");

        String extStorage = Environment.getExternalStorageDirectory().toString();
        Date date = new Date();
        CharSequence s = android.text.format.DateFormat.format("MM-dd-yy hh-mm-ss", date.getTime());
        // String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(date);
        String extension = "jpg";
        String nameComposed = "sensor_" + s + "." + extension;
        File file = new File(extStorage, nameComposed);
        if (checkSDCard() == false) {
            Toast.makeText(context, "SD card unmounted or not present", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            ImageOutStream = new FileOutputStream(file);
            System.out.println("Saving image:============================================");
            System.out.println("Saving image: HEADER 1 : " + String.format("%02X ", imageBuffer[0]));
            System.out.println("Saving image: HEADER 2 : " + String.format("%02X ", imageBuffer[1]));
            System.out.println("Saving image: HEADER 3 : " + String.format("%02X ", imageBuffer[2]));
            System.out.println("Saving image:============================================");
            System.out.println("Saving image: FOOTER 1 : " + String.format("%02X ", imageBuffer[finalIndex - 5]));
            System.out.println("Saving image: FOOTER 2 : " + String.format("%02X ", imageBuffer[finalIndex - 4]));
            System.out.println("Saving image: FOOTER 3 : " + String.format("%02X ", imageBuffer[finalIndex - 3]));

            System.out.println("Saving image: FINAL : " + String.format("%02X ", imageBuffer[finalIndex - 2]));

            ImageOutStream.write(imageBuffer, 0, (finalIndex - 2));
            //Saving images.

            saveImageInfo(nameComposed, "SENSOR");
            mHandler.obtainMessage(SYNC_PHOTO, nameComposed).sendToTarget();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.i(TAG, "Save file size = " + finalIndex);

        return true;
    }

    public void saveImageInfo(String name, String type) {
        Long tsLong = System.currentTimeMillis() / 1000;
        String timestamp = tsLong.toString();

        SQLiteDatabase db = conn.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Utilities.IMAGE_NAME, name);
        values.put(Utilities.IMAGE_TYPE, type);
        values.put(Utilities.IMAGE_TIMESTAMP, timestamp);

        long result = db.insert(Utilities.IMAGES_TABLE, null, values);
        Log.d("DB", "Saving data image: " + name + ", timestamp: " + timestamp);
        db.close();

    }

    public void saveMeasure(String ts, String type, String value, String unit, String location, String id) {
        Log.d("DB", "timestamp: " + ts + ", " + type + "," + value + "," + unit + "," + location + "," + id);
        SQLiteDatabase db = conn.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Utilities.FIELD_TIMESTAMP, ts);
        values.put(Utilities.FIELD_TYPE, type);
        values.put(Utilities.FIELD_VALUE, value);
        values.put(Utilities.FIELD_UNIT, unit);
        values.put(Utilities.FIELD_LOCATION, location);
        values.put(Utilities.FIELD_SENSORID, id);

        long result = db.insert(Utilities.MEASURE_TABLE, Utilities.FIELD_SENSORID, values);

        Log.d("DB", "RESULT :" + result);
        db.close();
    }

    public void processText(byte[] buffer, int n) {
        //                  //SENSOR DATA
        if (n <= 0) {
            return;
        }
        try {
            Log.i(TAG, "processText: " + n + " " + buffer.length);
            byte[] received = new byte[n];
            System.arraycopy(buffer, 0, received, 0, n);
            String input = new String(received);


            dataBuffer.append(input);

            Log.d(TAG, "DATA TO SPLIT RAW:" + dataBuffer);

            //Initial token
            int startIndex = dataBuffer.indexOf("--");
            int endOfLineIndex = dataBuffer.indexOf("#");

            if (startIndex >= 0) {

                Log.d(TAG, "DATA SLIDE INDEX: " + startIndex);

                Log.d(TAG, "DATA SLIDE INDEX FINAL : " + endOfLineIndex);


                if (endOfLineIndex > 0) {


                    String dataRow = dataBuffer.substring(startIndex + 2, endOfLineIndex);

                    Log.d(TAG, "DATA SLIDE : " + dataRow);
                    String[] parts = dataRow.split(",");

                    if (parts.length > 2) {
                        String sensorId = parts[0];
                        String type = parts[1];
                        String value = parts[2];
                        String unit = parts[3];
                        String location = parts[4];
                        Long tsLong = System.currentTimeMillis() / 1000;
                        String ts = tsLong.toString();
                        Log.d(TAG, "Saving sensor data in Database");
                        saveMeasure(ts, type, value, unit, location, sensorId);
                    }

                    String dataRowFinal = dataBuffer.substring(endOfLineIndex + 1);

                    dataBuffer.delete(0, dataBuffer.length());
                    Log.d(TAG, "DATA SLIDE BEFORE : " + dataBuffer);
                    dataBuffer.append(dataRowFinal);
                    Log.d(TAG, "DATA SLIDE AFTER : " + dataBuffer);

                }

            } else {
                //Purging
                dataBuffer.delete(0, dataBuffer.length());
            }

            mHandler.obtainMessage(SYNC_READ, input).sendToTarget();

        } catch (Exception e) {
            //Maybe it is a image
            e.printStackTrace();
            Log.d(TAG, "It is image data");
        }
    }

    public boolean processImage(byte[] buffer, int n) {

        if (n > 0) {
            //WARNING:
            //Each process needs to be with a "try-catch".
            //PHOTO DATA
            try {
                for (int i = 0; i < n; i++) {
                    //Start of image

                    if (fileIndex > 4
//                                        && i > 4
                            && imageBuffer[fileIndex - 5] == (byte) 0XF1
                            && imageBuffer[fileIndex - 4] == (byte) 0XF2
                            && imageBuffer[fileIndex - 3] == (byte) 0xF3
                            && imageBuffer[fileIndex - 2] == (byte) 0xFF
                            && imageBuffer[fileIndex - 1] == (byte) 0xD8
//                                        && buffer[i - 5] == (byte) 0X6F
//                                        && buffer[i - 4] == (byte) 0X74
//                                        && buffer[i - 3] == (byte) 0x6F
//                                        && buffer[i - 2] == (byte) 0xFF
//                                        && buffer[i - 1] == (byte) 0xD8
                            && buffer[i] == (byte) 0xFF) {
                        processingImage = true;

                        Log.d(TAG, "Saving image: SPLITER HEADER  1 : " + String.format("%02X ", imageBuffer[fileIndex - 5]));
                        Log.d(TAG, "Saving image: SPLITER HEADER  2 : " + String.format("%02X ", imageBuffer[fileIndex - 4]));
                        Log.d(TAG, "Saving image: SPLITER HEADER  3 : " + String.format("%02X ", imageBuffer[fileIndex - 3]));

                        fileIndex = 0;
                        finalIndex = 0;
                        validImage = false;
                        imageBuffer[fileIndex] = (byte) 0xFF;
                        fileIndex++;
                        imageBuffer[fileIndex] = (byte) 0xD8;
                        fileIndex++;
                        imageBuffer[fileIndex] = (byte) 0xFF;
                        fileIndex++;

                        Log.d(TAG, "Saving image: HEADER INDEX i : " + i);
                        Log.d(TAG, "Saving image: HEADER INDEX fileIndex: " + fileIndex);

                    } else {
                        imageBuffer[fileIndex] = buffer[i];

                        //When the image contain the Quantization Table(s)
                        if (fileIndex > 1
                                && imageBuffer[fileIndex - 1] == (byte) 0xFF
                                && imageBuffer[fileIndex] == (byte) 0xDB
                        ) {

                            Log.d(TAG, "Saving image: VALID IMAGE!!");
                            validImage = true;
                        }


                        if (fileIndex > 3
//                                            && i > 3
                                && finalIndex == 0
                                && imageBuffer[fileIndex - 4] == (byte) 0xFF
                                && imageBuffer[fileIndex - 3] == (byte) 0xD9
                                && imageBuffer[fileIndex - 2] == (byte) 0xF4
                                && imageBuffer[fileIndex - 1] == (byte) 0xF5
                                && imageBuffer[fileIndex] == (byte) 0xF6
//                                            && buffer[i - 4] == (byte) 0xFF
//                                            && buffer[i - 3] == (byte) 0xD9
//                                            && buffer[i - 2] == (byte) 0x70
//                                            && buffer[i - 1] == (byte) 0x68
//                                            && buffer[i] == (byte) 0x6F
                        ) {
                            Log.d(TAG, "Saving image: INDEX i : " + i);
                            Log.d(TAG, "Saving image: INDEX fileIndex: " + (fileIndex - 2));
                            Log.d(TAG, "Saving image: FOOTER  1 : " + String.format("%02X ", imageBuffer[fileIndex - 2]));
                            Log.d(TAG, "Saving image: FOOTER  2 : " + String.format("%02X ", imageBuffer[fileIndex - 1]));
                            Log.d(TAG, "Saving image: FOOTER  3 : " + String.format("%02X ", imageBuffer[fileIndex]));


                            finalIndex = fileIndex;

                            if (!validImage) {
                                Log.e(TAG, "processImage: Image not valid!");
                                fileIndex = 0;
                                finalIndex = 0;
                            } else {
                                if (saveImagetoSD()) {
                                    fileIndex = 0;
                                    processingImage = false;
                                    validImage = false;
                                    imageBuffer = new byte[MAX_VALUE];
                                    Log.d(TAG, "IMAGE SAVED!!!  ");
                                }
                                else {
                                    Log.e(TAG, "IMAGE NOT SAVED!!!");
                                }
                            }
                        }
                        fileIndex++;
                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "It is sensor data");
            } finally {
                return processingImage;
            }
        }
        return processingImage;
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {

            while (true) {
                byte[] buffer = new byte[1024];
                int n = serialPort.syncRead(buffer, 0);

                if (!processImage(Arrays.copyOf(buffer, buffer.length), n)) {
                    processText(Arrays.copyOf(buffer, buffer.length), n);
                }

            }
        }
    }
}
package com.example.jorge.blue.servicios;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.RequiresPermission;
import android.util.Log;


import com.example.jorge.blue.activities.UsbEventReceiverActivity;
import com.example.jorge.blue.entidades.ConexionSQLiteHelper;
import com.example.jorge.blue.utils.Utilities;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.jorge.blue.utils.Identifiers.alarmManager;
import static com.example.jorge.blue.utils.Identifiers.delta_time;
import static com.example.jorge.blue.utils.Identifiers.onService;
import static com.example.jorge.blue.utils.Identifiers.pendingIntent;

public class ServiceReceiver extends Service {
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    public static final int SYNC_READ = 3;
    public static final int SYNC_PHOTO = 4;
    private static final int BAUD_RATE = 2000000; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;
    private static final int BUFFER_SIZE = 64;

    private IBinder binder = new UsbBinder();

    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private UsbSerialPort port;
    //private StringBuilder dataBuffer = new StringBuilder();
    private String TAG = "USB-RECEIVER";
    private int MAX_VALUE = 1503600;
    byte[] imageBuffer = new byte[MAX_VALUE];  // 15KB reserved
    private FileOutputStream ImageOutStream;
    private StringBuilder dataBuffer = new StringBuilder();
    private static int fileIndex = 0;
    private static int finalIndex = -1;
    private boolean validImage = false;
    private boolean processingImage = false;
    private Thread readerThread;

    ConexionSQLiteHelper conn = new ConexionSQLiteHelper(this, Utilities.MEASURE_DATABASE_NAME, null, 1);

    private boolean serialPortConnected;

    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver attachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(UsbEventReceiverActivity.ACTION_USB_DEVICE_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port

            }
        }
    };

    private final BroadcastReceiver detachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Intent i = new Intent(ACTION_USB_NOT_SUPPORTED); // Broadcast disconnection to UserInterfaz
                context.sendBroadcast(i);

                if (readerThread != null) {
                    readerThread.interrupt();
                }
                serialPortConnected = false;

                stopSelf(); // Try to kill self, UserInterfaz needs to unbind before it happens
            }
        }
    };

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "SVC: Service created");

        this.context = this;
        serialPortConnected = false;
        ServiceReceiver.SERVICE_CONNECTED = true;

        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();

        readerThread = new ReadThread();
        readerThread.start();
    }

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
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceReceiver.SERVICE_CONNECTED = false;

        if (readerThread != null) {
            readerThread.interrupt();
        }

        Log.i(TAG, "SVC: Service destroyed");
        unregisterReceiver(attachReceiver);
        unregisterReceiver(detachReceiver);
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void findSerialPortDevice() {
        UsbDeviceConnection connection;
        UsbSerialDriver driver = null;

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        // Open a connection to the first available driver.
        for (UsbSerialDriver usd : availableDrivers) {
            if (usd == null) {
                continue;
            }
            UsbDevice udv = usd.getDevice();
            if (udv.getVendorId() == 9025) {
                driver = usd;
                break;
            }
        }

        if (driver == null) {
            stopSelf(); // No device found, kill service
            return;
        }
        connection = usbManager.openDevice(driver.getDevice());

        port = driver.getPorts().get(0);

        if (connection == null) return;

        try {
            port.open(connection);
            Log.i(TAG, "findSerialPortDevice: Port opened!");
            port.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            Log.d("SERIAL_ERR", e.getLocalizedMessage());
        }
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbEventReceiverActivity.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(attachReceiver, filter);

        IntentFilter f = new IntentFilter();
        f.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(detachReceiver, f);
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

                    readerThread = new ReadThread();
                    readerThread.start();

                    //
                    // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
                    // to be uploaded or not
                    try {
                        Thread.sleep(2000); // sleep some. YMMV with different chips.
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }

    /*public Bitmap StringToBitMap(String encodedString) {
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
        try {
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
                                fileIndex = 0;
                                finalIndex = 0;
                            } else if (finalIndex > 1000 && validImage) {
                                if (saveImagetoSD()) {
                                    fileIndex = 0;
                                    processingImage = false;
                                    validImage = false;
                                    imageBuffer = new byte[MAX_VALUE];
                                    Log.d(TAG, "IMAGE SAVED!!!  ");
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
    }*/

    private class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[1];
            byte[] datatype = new byte[1];
            byte[] timestamp = new byte[8];
            byte[] size = new byte[4];
            byte[] data = new byte[BUFFER_SIZE];

            int n;

            while (true) {
                if (Thread.interrupted()) { // End thread
                    serialPort.syncClose();
                    return;
                }

                try {

                    while ((n = port.read(buffer, 5000)) <= 0) ;
                    Log.i(TAG, "findSerialPortDevice: " + n + "---" + escape(buffer));

                    if (!("A".equals(new String(buffer)))) {
                        Log.e(TAG, "run: Expected A, received " + escape(buffer));
                        continue;
                    }

                    n = port.write("1\n".getBytes(), 1000);
                    Log.i(TAG, "run: Wrote = " + n);

                    port.purgeHwBuffers(true, false);

                    while ((n = port.read(datatype, 5000)) <= 0) ;
                    Log.i(TAG, "findSerialPortDevice2: " + n + "---" + escape(datatype));

                    while ((n = port.read(timestamp, 5000)) <= 0) ;
                    Log.i(TAG, "findSerialPortDevice3: " + n + "---" + escape(timestamp));

                    while ((n = port.read(size, 5000)) <= 0) ;
                    Log.i(TAG, "findSerialPortDevice4: " + n + "---" + escape(size));
                    int sizeInt = fromByteArray(size);
                    Log.i(TAG, "findSerialPortDevice4.5: SIZE = " + sizeInt);

                    byte[] allData = new byte[sizeInt];
                    int bytesCopied = 0;
                    while (bytesCopied < sizeInt) {
                        while ((n = port.read(data, 5000)) <= 0) ;
                        Log.i(TAG, "findSerialPortDevice5: DATA: " + n + "---" + escape(data));
                        copyToArr(data, allData, bytesCopied, n);

                        bytesCopied += n;
                    }

                    n = port.write("ZZZZ\n".getBytes(), 1000);

                    Log.i(TAG, "findSerialPortDevice6: ALL_DATA: " + escape(allData));
                } catch (Exception e) {
                    Log.d("SERIAL_ERR", e.getLocalizedMessage());
                }
            }
        }
    }

    public static String escape(byte[] data) {
        StringBuilder cbuf = new StringBuilder();
        for (byte b : data) {
            if (b >= 0x20 && b <= 0x7e) {
                cbuf.append((char) b);
            } else {
                cbuf.append(String.format("\\0x%02x", b & 0xFF));
            }
        }
        return cbuf.toString();
    }

    static int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    static void copyToArr(byte[] source, byte[] dst, int baseIndex, int numElems) {
        System.arraycopy(source, 0, dst, baseIndex, numElems);
    }
}
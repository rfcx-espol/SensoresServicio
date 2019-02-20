package com.example.jorge.blue.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.jorge.blue.R;
import com.example.jorge.blue.servicios.SendingService;
import com.example.jorge.blue.servicios.ServiceReceiver;
import com.example.jorge.blue.utils.Identifiers;
import com.facebook.battery.metrics.composite.CompositeMetrics;
import com.facebook.battery.metrics.composite.CompositeMetricsCollector;
import com.facebook.battery.metrics.cpu.CpuFrequencyMetrics;
import com.facebook.battery.metrics.cpu.CpuMetrics;
import com.facebook.battery.metrics.network.NetworkMetrics;
import com.facebook.battery.metrics.network.RadioStateMetrics;
import com.facebook.battery.metrics.time.TimeMetrics;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Set;

import static com.example.jorge.blue.utils.Identifiers.setAPIKey;

public class UserInterfaz extends AppCompatActivity {

    static String BAR_TITLE="Data Sender: ";

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ServiceReceiver.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case ServiceReceiver.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case ServiceReceiver.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case ServiceReceiver.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case ServiceReceiver.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private ServiceReceiver usbService;
    private TextView display;
    private MyHandler mHandler;
    public ImageView camImageView;
    public Button log;

    private final CompositeMetricsCollector mCollector =
            SensorApplication.INSTANCE.getMetricsCollector();
    private final CompositeMetrics mMetrics = mCollector.createMetrics();

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((ServiceReceiver.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interfaz);

        mHandler = new MyHandler(this);

        display = (TextView) findViewById(R.id.display);
        camImageView = (ImageView) findViewById(R.id.imageView);
        log = (Button) findViewById(R.id.log);
        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateContent();
            }
        });

        setAPIKey(this);
        getSupportActionBar().setTitle(BAR_TITLE+Identifiers.APIKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(ServiceReceiver.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void updateContent() {
        mCollector.getSnapshot(mMetrics);
        String separator = "========================";
        display.append(separator);
        String text = "\nSnapshot at " + System.currentTimeMillis() + ":\n" + mMetrics.getMetric(TimeMetrics.class);
        display.append(text);
        String text2 = ":\n" + mMetrics.getMetric(CpuMetrics.class);
        display.append(text2);
       // String text3 = ":\n" + mMetrics.getMetric(CpuFrequencyMetrics.class);
        //display.append(text3);
        String text4 = ":\n" + mMetrics.getMetric(NetworkMetrics.class);
        display.append(text4);

        Log.d("BatteryMetrics", text);
        Log.d("BatteryMetrics", text2);
       // Log.d("BatteryMetrics", text3);
        Log.d("BatteryMetrics", text4);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!ServiceReceiver.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceReceiver.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(ServiceReceiver.ACTION_NO_USB);
        filter.addAction(ServiceReceiver.ACTION_USB_DISCONNECTED);
        filter.addAction(ServiceReceiver.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(ServiceReceiver.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<UserInterfaz> mActivity;

        public MyHandler(UserInterfaz activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServiceReceiver.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().display.append(data);
                    break;
                case ServiceReceiver.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case ServiceReceiver.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case ServiceReceiver.SYNC_READ:
                    String buffer = (String) msg.obj;
                    mActivity.get().display.append(buffer);
                    break;
                case ServiceReceiver.SYNC_PHOTO:

                    String photoName = (String) msg.obj;
                    String extStorage = Environment.getExternalStorageDirectory().toString();
                    File file = new File(extStorage, photoName);

                    String myJpgPath = file.getPath();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bm = BitmapFactory.decodeFile(myJpgPath, options);
                    if(bm != null){
                        int width = bm.getWidth();
                        int height = bm.getHeight();
                        Matrix matrix = new Matrix();
                        float scaleWidth = ((float)mActivity.get().camImageView.getWidth())/ width;
                        float scaleHeight = scaleWidth;
                        matrix.postScale(scaleWidth, scaleHeight);
                        Bitmap result = Bitmap.createBitmap(bm, 0, 0, width,
                                height, matrix, true);
                        mActivity.get().camImageView.setImageBitmap(result);
                    }

                    break;
            }
        }
    }
}
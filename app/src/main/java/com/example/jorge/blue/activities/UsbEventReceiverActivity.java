package com.example.jorge.blue.activities;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.example.jorge.blue.servicios.ServiceReceiver;

public class UsbEventReceiverActivity extends Activity {
    public static final String ACTION_USB_DEVICE_ATTACHED = "com.example.jorge.blue.ACTION_USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DEVICE_DETACHED = "com.example.jorge.blue.ACTION_USB_DEVICE_DETACHED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("None", "onResume: baap");

        Intent intent = getIntent();
        Log.i("None", "onResume: " + intent);
        if (intent != null) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                // Create a new intent and put the usb device in as an extra
                Intent broadcastIntent = new Intent(ACTION_USB_DEVICE_ATTACHED);
                broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                // Broadcast this event so we can receive it
                //sendBroadcast(broadcastIntent);
                startService(new Intent(this, ServiceReceiver.class));
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {

                // Create a new intent and put the usb device in as an extra
                Intent broadcastIntent = new Intent(ACTION_USB_DEVICE_DETACHED);

                // Broadcast this event so we can receive it
                //sendBroadcast(broadcastIntent);
                stopService(new Intent(this, ServiceReceiver.class));
            }
        }

        // Close the activity
        finish();
    }
}
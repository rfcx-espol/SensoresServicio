package com.example.jorge.blue.utils;

import android.support.annotation.Nullable;
import android.util.Log;
import com.facebook.battery.reporter.core.SystemMetricsReporter;

/** Poor man's analytics: also known as Logcat. */
public class Event implements SystemMetricsReporter.Event {
    @Override
    public boolean isSampled() {
        return false;
    }

    @Override
    public void acquireEvent(@Nullable String moduleName, String eventName) {
        Log.i("SensorApplication", "New event: {");
    }

    @Override
    public void add(String key, String value) {
        Log.i("SensorApplication", key + ":" + value);
    }

    @Override
    public void add(String key, int value) {
        Log.i("SensorApplication", key + ":" + value);
    }

    @Override
    public void add(String key, long value) {
        Log.i("SensorApplication", key + ":" + value);
    }

    @Override
    public void add(String key, double value) {
        Log.i("SensorApplication", key + ":" + value);
    }

    @Override
    public void logAndRelease() {
        Log.i("SensorApplication", "}");
    }
}

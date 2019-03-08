package com.example.jorge.blue.activities;

    import android.app.Activity;
    import android.app.Application;
    import android.os.Build;
    import android.os.Bundle;
    import android.util.Log;

    import com.example.jorge.blue.utils.Event;
    import com.facebook.battery.metrics.composite.CompositeMetrics;
    import com.facebook.battery.metrics.composite.CompositeMetricsCollector;
    import com.facebook.battery.metrics.core.StatefulSystemMetricsCollector;
    import com.facebook.battery.metrics.cpu.CpuFrequencyMetrics;
    import com.facebook.battery.metrics.cpu.CpuFrequencyMetricsCollector;
    import com.facebook.battery.metrics.cpu.CpuMetrics;
    import com.facebook.battery.metrics.cpu.CpuMetricsCollector;
    import com.facebook.battery.metrics.healthstats.HealthStatsMetrics;
    import com.facebook.battery.metrics.healthstats.HealthStatsMetricsCollector;
    import com.facebook.battery.metrics.network.NetworkMetrics;
    import com.facebook.battery.metrics.network.NetworkMetricsCollector;
    import com.facebook.battery.metrics.network.RadioStateCollector;
    import com.facebook.battery.metrics.network.RadioStateMetrics;
    import com.facebook.battery.metrics.time.TimeMetrics;
    import com.facebook.battery.metrics.time.TimeMetricsCollector;
    import com.facebook.battery.reporter.composite.CompositeMetricsReporter;
    import com.facebook.battery.reporter.core.SystemMetricsReporter;
    import com.facebook.battery.reporter.cpu.CpuFrequencyMetricsReporter;
    import com.facebook.battery.reporter.cpu.CpuMetricsReporter;
    import com.facebook.battery.reporter.healthstats.HealthStatsMetricsReporter;
    import com.facebook.battery.reporter.network.NetworkMetricsReporter;
    import com.facebook.battery.reporter.network.RadioStateReporter;
    import com.facebook.battery.reporter.time.TimeMetricsReporter;
    import com.facebook.battery.serializer.composite.CompositeMetricsSerializer;
    import com.facebook.battery.serializer.core.SystemMetricsSerializer;
    import com.facebook.battery.serializer.cpu.CpuFrequencyMetricsSerializer;
    import com.facebook.battery.serializer.cpu.CpuMetricsSerializer;
    import com.facebook.battery.serializer.healthstats.HealthStatsMetricsSerializer;
    import com.facebook.battery.serializer.network.NetworkMetricsSerializer;
    import com.facebook.battery.serializer.time.TimeMetricsSerializer;
    import java.io.*;

public class SensorApplication extends Application
        implements Application.ActivityLifecycleCallbacks {
    private static final String LAST_SNAPSHOT = "lastsnapshot";

    public static volatile SensorApplication INSTANCE;
    private CompositeMetricsCollector mMetricsCollector;

    private StatefulSystemMetricsCollector<CompositeMetrics, CompositeMetricsCollector>
            mStatefulCollector;
    private CompositeMetricsReporter mMetricsReporter;
    private CompositeMetricsSerializer mMetricsSerializer;
    private final SystemMetricsReporter.Event mEvent = new Event();

    private void init() {


        CompositeMetricsCollector.Builder collectorBuilder =
                new CompositeMetricsCollector.Builder()
                        .addMetricsCollector(TimeMetrics.class, new TimeMetricsCollector())
                        .addMetricsCollector(CpuFrequencyMetrics.class, new CpuFrequencyMetricsCollector())
                        .addMetricsCollector(CpuMetrics.class, new CpuMetricsCollector())
                        .addMetricsCollector(NetworkMetrics.class, new NetworkMetricsCollector(this))
                        .addMetricsCollector(NetworkMetrics.class, new NetworkMetricsCollector(this))
                        .addMetricsCollector(RadioStateMetrics.class, new RadioStateCollector(this));
        if (Build.VERSION.SDK_INT >= 24) {
            collectorBuilder.addMetricsCollector(
                    HealthStatsMetrics.class, new HealthStatsMetricsCollector(this));
        }
        mMetricsCollector = collectorBuilder.build();


        mMetricsReporter =
                new CompositeMetricsReporter()
                        .addMetricsReporter(TimeMetrics.class, new TimeMetricsReporter())
                        .addMetricsReporter(CpuMetrics.class, new CpuMetricsReporter())
                        .addMetricsReporter(CpuFrequencyMetrics.class, new CpuFrequencyMetricsReporter())
                        .addMetricsReporter(NetworkMetrics.class, new NetworkMetricsReporter())
                        .addMetricsReporter(RadioStateMetrics.class, new RadioStateReporter());
        if (Build.VERSION.SDK_INT >= 24) {
            mMetricsReporter.addMetricsReporter(
                    HealthStatsMetrics.class, new HealthStatsMetricsReporter());
        }

        mMetricsSerializer =
                new CompositeMetricsSerializer()
                        .addMetricsSerializer(TimeMetrics.class, new TimeMetricsSerializer())
                        .addMetricsSerializer(CpuMetrics.class, new CpuMetricsSerializer())
                        .addMetricsSerializer(CpuFrequencyMetrics.class, new CpuFrequencyMetricsSerializer())
                        .addMetricsSerializer(NetworkMetrics.class, new NetworkMetricsSerializer())
                        .addMetricsSerializer(RadioStateMetrics.class, new SystemMetricsSerializer<RadioStateMetrics>() {
                            @Override
                            public void serializeContents(RadioStateMetrics metrics, DataOutput output) throws IOException {

                                output.writeLong(metrics.mobileLowPowerActiveS);
                                output.writeLong(metrics.mobileHighPowerActiveS);
                                output.writeLong(metrics.mobileRadioWakeupCount);
                                output.writeLong(metrics.wifiActiveS);
                                output.writeLong(metrics.wifiRadioWakeupCount);
                            }

                            @Override
                            public boolean deserializeContents(RadioStateMetrics metrics, DataInput input) throws IOException {
                                metrics.mobileLowPowerActiveS = input.readLong();
                                metrics.mobileHighPowerActiveS = input.readLong();
                                metrics.mobileRadioWakeupCount = input.readInt();
                                metrics.wifiActiveS = input.readLong();
                                metrics.wifiRadioWakeupCount = input.readInt();
                                return true;
                            }
                        });
        if (Build.VERSION.SDK_INT >= 24) {
            mMetricsSerializer.addMetricsSerializer(
                    HealthStatsMetrics.class, new HealthStatsMetricsSerializer());
        }

        mStatefulCollector = new StatefulSystemMetricsCollector<>(mMetricsCollector);
    }

    public CompositeMetricsCollector getMetricsCollector() {
        return mMetricsCollector;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        init();

        registerActivityLifecycleCallbacks(this);

        try (DataInputStream input = new DataInputStream(new FileInputStream(openFile()))) {
            CompositeMetrics metrics = mMetricsCollector.createMetrics();

            // Note -- this reads in from the last serialized value
            mMetricsSerializer.deserialize(metrics, input);

            // Note -- We've been careful to have good, readable `toString` implementations for metrics
            Log.i("SensorApplication", "Last saved snapshot:\n" + metrics.toString());

        } catch (IOException ioe) {
            Log.e("SensorApplication", "Failed to deserialize", ioe);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {
        // Note: Triggering an update / difference on transition
        logMetrics("background");
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Note: Triggering an update on transition
        logMetrics("foreground");
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    private void logMetrics(String tag) {
        // Note -- this gets the difference from the last call / initialization of the StatefulCollector
        CompositeMetrics update = mStatefulCollector.getLatestDiffAndReset();

        // Check out the Event class in this folder: it should be able to wrap most analytics
        // implementations comfortably; this one simply logs everything to logcat.
        mEvent.acquireEvent(null, "BatteryMetrics");
        if (mEvent.isSampled()) {
            mEvent.add("dimension", tag);
            mMetricsReporter.reportTo(update, mEvent);
            mEvent.logAndRelease();
        }

        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(openFile()))) {
            // Save data as required, as cheaply as possible.
            mMetricsSerializer.serialize(update, output);
        } catch (IOException ioe) {
            Log.e("SensorApplication", "Failed to serialize", ioe);
        }
    }

    private File openFile() {
        return new File(getFilesDir(), LAST_SNAPSHOT);
    }
}
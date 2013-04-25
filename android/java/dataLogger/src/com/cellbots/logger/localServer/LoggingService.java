
package com.cellbots.logger.localServer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.cellbots.logger.GpsManager;
import com.cellbots.logger.LoggerApplication;
import com.cellbots.logger.WapManager;
import com.cellbots.logger.GpsManager.GpsManagerListener;
import com.cellbots.logger.WapManager.ScanResults;
import com.cellbots.logger.localServer.LocalHttpServer.HttpCommandServerListener;
import com.cellbots.logger.localServer.Telemetry.DataPacket.Builder;
import com.cellbots.logger.localServer.XmppManager.XmppMessageListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Background service that performs logging and serves up the results as an HTTP
 * server.
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class LoggingService extends Service implements HttpCommandServerListener {
    // FILL THESE OUT TO USE XMPP!
    private static final String XMPP_PROTOBUF_RECEIVER_BOT = "";    
    public static final String GMAIL_ACCOUNT = "";
    public static final String GMAIL_PASSWORD = "";

    public static final String EXTRA_COMMAND = "COMMAND";
    public static final int EXTRA_COMMAND_STOP = 0;
    public static final int EXTRA_COMMAND_START = 1;

    private static final String TAG = "LoggingService";

    private LoggerApplication application;

    // FLAGS
    // TODO: Make these configurable!
    private boolean mWriteToFile = true; // Switch this to true to log to files
                                         // in addition to displaying through
                                         // HTTP.

    private SensorManager mSensorManager;
    private List<Sensor> sensors;
    private volatile Boolean mIsLoggerRunning = false;

    private BufferedWriter mBatteryTempWriter;
    private BufferedWriter mBatteryLevelWriter;
    private BufferedWriter mBatteryVoltageWriter;
    private BufferedWriter mWifiWriter;
    private HashMap<String, BufferedWriter> sensorLogFileWriters;
    private HashMap<String, String> lastSeenValues;
    private BufferedWriter mGpsLocationWriter;
    private BufferedWriter mGpsStatusWriter;
    private BufferedWriter mGpsNmeaWriter;
    private GpsManager mGpsManager;

    private LocalHttpServer httpServer;
    private XmppManager xmppHandler;
    private long mLastXmppUpdateTime = 0;
    private TelemetrySnapshot mTelemetrySnapshot;

    private String mDirectoryName;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        application = (LoggerApplication) getApplication();
        mDirectoryName = application.getDataLoggerPath();
        if (intent != null) {
            switch (intent.getIntExtra(EXTRA_COMMAND, EXTRA_COMMAND_STOP)) {
                case EXTRA_COMMAND_START:
                    if (!mIsLoggerRunning) {
                        runLoggerService();
                        httpServer = new LocalHttpServer("cellbots/httpserver/files", 8080, this);
                        if (GMAIL_ACCOUNT.length() > 0) {
                            xmppHandler = new XmppManager(
                                    this, mXmppMessageListener, GMAIL_ACCOUNT, GMAIL_PASSWORD);
                            xmppHandler.connect();
                        }
                    }
                    break;
                default:
                    stopSelf();
                    break;
            }
        }
        return START_STICKY;
    }

    XmppMessageListener mXmppMessageListener = new XmppMessageListener() {
            @Override
        public void onMessageReceived(String from, String message) {
            xmppHandler.sendMessage(from, getLoggerStatus());
        }
    };

    private Runnable sendUpdatesToXmppRunnable = new Runnable() {
            @Override
        public void run() {
            while (mIsLoggerRunning && (GMAIL_ACCOUNT.length() > 0)) {
                if ((xmppHandler != null) && (mTelemetrySnapshot != null) && (mLastXmppUpdateTime + 15000 < System.currentTimeMillis())) {
                    Log.e("Message to bot", "Sending...");
                    xmppHandler.sendMessage(XMPP_PROTOBUF_RECEIVER_BOT, "/prot " + mTelemetrySnapshot.getBase64EncodedProtobufDataPacket());
                    mLastXmppUpdateTime = System.currentTimeMillis();
                    Log.e("Message to bot", "OK");
                }
            }
        }
    };

    private void runLoggerService() {
        mIsLoggerRunning = true;
        mTelemetrySnapshot = new TelemetrySnapshot();
        lastSeenValues = new HashMap<String, String>();
        Log.e(TAG, "Starting logging service");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        initSensorLogFiles();
        initGps();
        new Thread(sendUpdatesToXmppRunnable).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsLoggerRunning = false;
        if ((sensors != null) && (mSensorManager != null)) {
            // Unregister sensor listeners
            for (Sensor s : sensors) {
                mSensorManager.unregisterListener(mSensorEventListener, s);
            }
        }
        if (mGpsManager != null){
            mGpsManager.shutdown();
        }
        if (xmppHandler != null) {
            xmppHandler.disconnect();
        }
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
            @Override
        public void onSensorChanged(SensorEvent event) {
            mTelemetrySnapshot.updateSensor(event);
            Sensor sensor = event.sensor;
            if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                // Gyroscope doesn't really have a notion of accuracy.
                // Due to a bug in Android, the gyroscope incorrectly returns
                // its status as unreliable. This can be safely ignored and does
                // not impact the accuracy of the readings.
                event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            }
            synchronized (mIsLoggerRunning) {
                if (mIsLoggerRunning) {
                    String valuesStr = "";
                    for (int i = 0; i < event.values.length; i++) {
                        valuesStr = valuesStr + event.values[i] + ",";
                    }
                    final String sensorName = sensor.getName();
                    final String lastSeenValue = event.timestamp + "," + event.accuracy + ","
                            + valuesStr;
                    lastSeenValues.put(sensorName, lastSeenValue);
                    // Log.d(TAG, sensorName + ":" + lastSeenValue);
                    BufferedWriter writer = sensorLogFileWriters.get(sensorName);
                    if (writer != null) {
                        try {
                            writer.write(lastSeenValue + "\n");
                            writer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

            @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private BroadcastReceiver batteryBroadcastReceiver = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mIsLoggerRunning) {
                if (!mIsLoggerRunning) {
                    return;
                }
                String value = "";
                long currentTime = System.currentTimeMillis();
                int batteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                try {
                    value = currentTime + "," + batteryTemp;
                    lastSeenValues.put("BatteryTemp", value);
                    if (mBatteryTempWriter != null) {
                        mBatteryTempWriter.write(value + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Log the battery level
                int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                try {
                    value = currentTime + "," + batteryLevel;
                    lastSeenValues.put("BatteryLevel", value);
                    if (mBatteryLevelWriter != null) {
                        mBatteryLevelWriter.write(value + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Log the battery voltage level
                int batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                try {
                    value = currentTime + "," + batteryVoltage;
                    lastSeenValues.put("BatteryVoltage", value);
                    if (mBatteryVoltageWriter != null) {
                        mBatteryVoltageWriter.write(value + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private WapManager.WapManagerListener mWifiListener = new WapManager.WapManagerListener() {
            @Override
        public void onScanResults(long timestamp, ScanResults results) {
            synchronized (mIsLoggerRunning) {
                if (!mIsLoggerRunning)
                    return;
            }
            try {
                // Convert results to a json object
                JSONObject obj = new JSONObject();
                JSONObject resultsObj = new JSONObject(results);
                obj.put("timestamp", timestamp);
                obj.put("results", resultsObj);

                lastSeenValues.put("Wifi", timestamp + "," + resultsObj.toString());
                if (mWifiWriter != null) {
                    // Write that object to a file
                    mWifiWriter.write(obj.toString());
                    mWifiWriter.write("\n");
                }
            } catch (JSONException e) {
                Log.e("LoggerActivity", "Error logging wifi results. JSON Error");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("LoggerActivity", "Error logging wifi results. IO error");
                e.printStackTrace();
            }
        }
    };

    private void initSensorLogFiles() {
        sensorLogFileWriters = new HashMap<String, BufferedWriter>();

        if (mWriteToFile) {
            File directory = new File(mDirectoryName);
            if (!directory.exists() && !directory.mkdirs()) {
                try {
                    throw new IOException(
                            "Path to file could not be created. " + directory.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Directory could not be created. " + e.toString());
                }
            }
        }

        for (int i = 0; i < sensors.size(); i++) {
            Sensor s = sensors.get(i);
            if (mWriteToFile) {
                String sensorFilename = mDirectoryName + s.getName().replaceAll(" ", "_") + "_"
                        + application.getFilePathUniqueIdentifier() + ".txt";
                File file = new File(sensorFilename);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    sensorLogFileWriters.put(s.getName(), writer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // GPS is another special case since it is not a real sensor
                mGpsLocationWriter = createBufferedWriter("/GpsLocation_", mDirectoryName);
                mGpsStatusWriter = createBufferedWriter("/GpsStatus_", mDirectoryName);
                mGpsNmeaWriter = createBufferedWriter("/GpsNmea_", mDirectoryName);
            }
            mSensorManager.registerListener(
                    mSensorEventListener, s, SensorManager.SENSOR_DELAY_GAME);
        }
        /*
         * // The battery is a special case since it is not a real sensor
         * mBatteryTempWriter = createBufferedWriter("/BatteryTemp_",
         * directoryName); mBatteryLevelWriter =
         * createBufferedWriter("/BatteryLevel_", directoryName);
         * mBatteryVoltageWriter = createBufferedWriter("/BatteryVoltage_",
         * directoryName); // GPS is another special case since it is not a real
         * sensor mGpsLocationWriter = createBufferedWriter("/GpsLocation_",
         * directoryName); mGpsStatusWriter =
         * createBufferedWriter("/GpsStatus_", directoryName); mGpsNmeaWriter =
         * createBufferedWriter("/GpsNmea_", directoryName); // Wifi is another
         * special case mWifiWriter = createBufferedWriter("/Wifi_",
         * directoryName);
         */
    }

    /**
     * Creates a new BufferedWriter.
     * 
     * @param prefix The prefix for the file that we're writing to.
     * @return A BufferedWriter for a file in the specified directory. Null if
     *         creation failed.
     */
    private BufferedWriter createBufferedWriter(String prefix, String directoryName) {
        String filename = directoryName + prefix + application.getFilePathUniqueIdentifier()
                + ".txt";
        File file = new File(filename);
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bufferedWriter;
    }

    private void initGps() {
        mGpsManager = new GpsManager(this, new GpsManagerListener() {
                @Override
            public void onGpsLocationUpdate(long time, float accuracy, double latitude,
                    double longitude, double altitude, float bearing, float speed) {
                try {
                    mTelemetrySnapshot.updateLocation(latitude, longitude, altitude);
                    if (mWriteToFile) {
                        mGpsLocationWriter.write(
                                time + "," + accuracy + "," + latitude + "," + longitude + ","
                                + altitude + "," + bearing + "," + speed + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

                @Override
            public void onGpsNmeaUpdate(long time, String nmeaString) {
                try {
                    if (mWriteToFile) {
                        mGpsNmeaWriter.write(time + "," + nmeaString + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

                @Override
            public void onGpsStatusUpdate(
                    long time, int maxSatellites, int actualSatellites, int timeToFirstFix) {
                try {
                    if (mWriteToFile) {
                        mGpsStatusWriter.write(
                                time + "," + maxSatellites + "," + actualSatellites + ","
                                + timeToFirstFix + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /*
     * (non-Javadoc)
     * @see
     * com.cellbots.logger.localServer.HttpCommandServer.HttpCommandServerListener
     * #onRequest(java.lang.String, java.lang.String[], java.lang.String[],
     * byte[])
     */
    @Override
    public void onRequest(String req, String[] keys, String[] values, byte[] data) {
        Log.e("Server debug", "Request received:" + req);
    }

    @Override
    public String getLoggerStatus() {
        StringBuilder statusMessage = new StringBuilder();
        Iterator<String> sensorNamesIt = lastSeenValues.keySet().iterator();
        while (sensorNamesIt.hasNext()) {
            final String name = sensorNamesIt.next();
            statusMessage.append(name);
            statusMessage.append(":");
            statusMessage.append(lastSeenValues.get(name));
            statusMessage.append("\n");
        }
        return statusMessage.toString();
    }

    public void addLogEntryToCustomSensor(final String sensorName, final String sensorReadings) {
        final String lastSeenValue = System.currentTimeMillis() + "," + sensorReadings;
        lastSeenValues.put(sensorName, lastSeenValue);
        BufferedWriter writer = sensorLogFileWriters.get(sensorName);
        if (mWriteToFile && (writer == null)) {
            String sensorFilename = mDirectoryName + sensorName + "_"
                    + application.getFilePathUniqueIdentifier() + ".txt";
            File file = new File(sensorFilename);
            try {
                writer = new BufferedWriter(new FileWriter(file));
                sensorLogFileWriters.put(sensorName, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (writer != null) {
            try {
                writer.write(lastSeenValue + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class Stub extends ILoggingService.Stub {
        @Override
        public void addLogEntry(String sensorName, String sensorReadings)
                throws RemoteException {
            addLogEntryToCustomSensor(sensorName, sensorReadings);
        }
    }

    private final Stub mBinder = new Stub();
}

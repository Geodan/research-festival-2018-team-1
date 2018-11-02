package nl.geodan.steward;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

public class SensorService extends Service implements SensorEventListener {
    public static int counter = 0;
    public static double longitude = 0d;
    public static double latitude = 0d;
    public static double accuracy = 0d;
    public static double bearing = 0d;
    public static boolean hasConnection = false;
    public static String ip = "";

    private static Timer timer;
    private static TimerTask timerTask;

    private static final String TAG = "STEWARDSERVICE";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;

    private static PahoMqttClient pahoMqttClient;
    private static MqttAndroidClient mqttAndroidClient;
    private boolean mqttConnecting;

    private static final String TAG_FOREGROUND_SERVICE = "STEWARDCHANNEL";

    private SensorManager SensorManager;


    public synchronized void setCounter(int newValue) {
        counter = newValue;
    }

    public synchronized void setAccuracy(double newValue) {
        accuracy = newValue;
    }

    public synchronized void setBearing(double newValue) {
        bearing = newValue;
    }

    public synchronized void setLongitude(double newValue) {
        longitude = newValue;
    }

    public synchronized void setLatitude(double newValue) {
        latitude = newValue;
    }

    public synchronized void setHasConnection(boolean newValue) {
        hasConnection = newValue;
    }

    public synchronized void setIp(String newValue) {
        ip = newValue;
    }

    public SensorService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
    }

    public SensorService() {
    }

    public String getStewardName(){
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("stewardName", "steward1");
    }

    public String getUniqueId(){
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("uuid", "empty");
    }

    public String getBroker(){
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("broker", "tcp://iot.eclipse.org:1883");
    }

    public int getUpdateFrequency(){
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("updateFrequency", 1000);
    }


        @Override
    public int onStartCommand(Intent Intent, int flags, int startId) {
        super.onStartCommand(Intent, flags, startId);
        startMqtt();
        startLocation();
        startCompass();
        startTimer();

        //return START_STICKY;
        Log.d(TAG_FOREGROUND_SERVICE, "Start foreground service.");
        createNotificationChannel();

        // Create notification default intent.
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, Intent, 0);

        // Create notification builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TAG_FOREGROUND_SERVICE);

        // Make notification show big text.
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle("ArenA Steward Tracker");
        bigTextStyle.bigText("Status: Tracking");
        builder.setStyle(bigTextStyle);

        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.route);

        // Make the notification max priority.
        builder.setPriority(Notification.PRIORITY_MAX);
        // Make head-up notification.
        builder.setFullScreenIntent(pendingIntent, true);

        // Build the notification.
        Notification notification = builder.build();

        // Start foreground service.
        startForeground(1, notification);

        return super.onStartCommand(Intent, flags, startId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "steward";
            String description = "tracking steward";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(TAG_FOREGROUND_SERVICE, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        Log.i("EXIT", "ondestroy!");
       // Intent broadcastIntent = new Intent("nl.geodan.steward.RestartSensor");
       // sendBroadcast(broadcastIntent);
        stoptimertask();

        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }

        super.stopSelf();
        super.onDestroy();
    }

    public void startCompass(){
        SensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        SensorManager.registerListener(this, SensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
    }

    public void startMqtt(){
        if(!hasConnection){
            return;
        }

        mqttConnecting = true;
        pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), getBroker(), getUniqueId());
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                mqttConnecting = false;
            }
            @Override
            public void connectionLost(Throwable throwable) {}
            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {}
            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {}
        });
        try {
            mqttAndroidClient.connect(pahoMqttClient.getMqttConnectionOption());
        } catch (MqttException e) {
            e.printStackTrace();
            mqttConnecting = false;
        }
    }

    public void startLocation(){
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, getUpdateFrequency(), getUpdateFrequency()); //
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
            updateConnection();

            if(!hasConnection){
                return;
            }

            setAccuracy(Math.floor(mLocationListeners[0].mLastLocation.getAccuracy()));
            setLongitude(longitude = mLocationListeners[0].mLastLocation.getLongitude());
            setLatitude(mLocationListeners[0].mLastLocation.getLatitude());

            if(!mqttAndroidClient.isConnected() && !mqttConnecting){
                startMqtt();
                return;
            }

            if(mqttConnecting || longitude == 0d || latitude == 0d){
                return;
            }

            setCounter(counter + 1);
            String msg = String.valueOf(longitude) + "," + String.valueOf(latitude) + "," + String.valueOf(accuracy) + "," + String.valueOf(bearing) + "," + ip + "," + getStewardName() + "," + getDateTimeString();

            try {
                pahoMqttClient.publishMessage(mqttAndroidClient, msg, 1, "arena/" + getUniqueId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            }
        };
    }

    public String publishIncident(String id, String stewardName, String incident, String description){
        if(!hasConnection || !mqttAndroidClient.isConnected()){
            return "No connection, incident not created";
        }

        String msg = id + "," + stewardName + "," + incident + "," + description + "," + String.valueOf(longitude) + "," + String.valueOf(latitude) + "," + getDateTimeString();
        try {
            pahoMqttClient.publishMessage(mqttAndroidClient, msg, 1, "arena/incidents");
        } catch (Exception e) {
            return e.getMessage();
        }

        return null;
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
        new LocationListener(LocationManager.GPS_PROVIDER),
        new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    public void updateConnection(){
        String localIp = getLocalIpAddress();
        boolean hasConnection = localIp == null ? false : true;

        if(this.hasConnection != hasConnection){
            setHasConnection(hasConnection);
            if(this.hasConnection){ // connection restored, reconnect mqtt
                startMqtt();
            }
        }

        localIp = localIp == null ? "0.0.0.0" : localIp;
        setIp(localIp);
    }

    private String getDateTimeString(){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return df.format(Calendar.getInstance().getTime());
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] mRotationMatrix = new float[9];
        float[] mChangedRotationMatrix = new float[9];
        float[] mOrientation = new float[9];

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mChangedRotationMatrix);
        SensorManager.getOrientation(mChangedRotationMatrix, mOrientation);
        float magneticHeading = (float) Math.toDegrees(mOrientation[0]);

        //change range 0,180/-180,-1 -> 0-360
        if(magneticHeading < 0){
            magneticHeading = 360 - Math.abs(magneticHeading);
        }

        setBearing(Math.floor(magneticHeading));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider)
        {
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
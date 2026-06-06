package com.dnrcpw.cpwmobilepdf.activities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.util.Log;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class TrackingService extends Service {
    private static final String CHANNEL_ID = "LocationTrackingChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    // Global tracker configuration (Default values)
    private long currentIntervalMillis = 10000; // 2 seconds default for testing     10000; // 10 seconds default
    private long currentFastestIntervalMillis = 5000; // 5 seconds default
    private boolean isAutoAdjustEnabled = true; // Toggle for speed-based adjustment
    private float lastSpeedMps = 0.0f;
    // used to write track data to the database
    public static final String ACTION_TOGGLE_RECORDING = "com.dnrcpw.cpwmobilepdf.TOGGLE_RECORDING";
    public static final String EXTRA_IS_RECORDING = "extra_is_recording";
    public static final String EXTRA_CURRENT_TRACK_ID = "extra_current_track_id";
    private DBHandler dbHelper;
    // State flag controlling whether updates write to SQLite
    private boolean isRecordingTracks = false;
    private int currentTrackId = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHandler(getApplicationContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d("TrackingService", "LocationResult is null");
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    double altitude = location.hasAltitude() ? location.getAltitude() : -1.0;
                    float accuracy = location.getAccuracy(); // Get accuracy in meters
                    // Default to -1.0f if the location object does not contain a valid bearing
                    float bearing = location.hasBearing() ? location.getBearing() : -1.0f;

                    // Extract speed in meters per second (requires GPS)
                    float speed = location.hasSpeed() ? location.getSpeed() : 0.0f;

                    if (isAutoAdjustEnabled) {
                        adjustIntervalBasedOnSpeed(speed);
                    }

                    // Create an intent with a custom action string to update current location/distance to map
                    Intent intent = new Intent("ACTION_LOCATION_UPDATE");
                    intent.putExtra("extra_latitude", latitude);
                    intent.putExtra("extra_longitude", longitude);
                    intent.putExtra("extra_altitude", altitude);
                    intent.putExtra("extra_accuracy", accuracy);
                    intent.putExtra("extra_bearing", bearing);

                    // Broadcast to the system (restricted to your app package for security)
                    intent.setPackage(getPackageName());
                    sendBroadcast(intent);

                    // Conditionally save to SQLite database if recording is toggled on
                    if (isRecordingTracks) {
                        dbHelper.updateTrack(
                                latitude,
                                longitude,
                                currentTrackId
                        );
                    }
                }
            }
        };
    }

    // Example public method your second activity might want to call
    public float getTotalDistance() {
        // Assuming you track totalDistanceTraveled globally here
        return 1500.5f;
    }
    public int getCurrentTrackId(){
        return currentTrackId;
    }
    private void adjustIntervalBasedOnSpeed(float speedMps) {
        long newInterval;
        long newFastest;

        if (speedMps > 11.1) { // Faster than 25 mph / 40 kmh (Driving)
            newInterval = 2000;  // 2 seconds
            newFastest = 1000;
        } else if (speedMps > 1.5) { // Running / Cycling
            newInterval = 5000;  // 5 seconds
            newFastest = 2000;
        } else { // Walking / Stationary
            newInterval = 15000; // 15 seconds
            newFastest = 7000;
        }

        // Only rebuild request if state shifts drastically to avoid endless loop resetting
        if (Math.abs(speedMps - lastSpeedMps) > 2.0f) {
            lastSpeedMps = speedMps;
            changeLocationInterval(newInterval, newFastest);
            Log.d("TrackingService", "Auto-adjusted interval to: " + newInterval + "ms");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // Handle requests to turn recording on or off
            if (ACTION_TOGGLE_RECORDING.equals(intent.getAction())) {
                isRecordingTracks = intent.getBooleanExtra(EXTRA_IS_RECORDING, false);
                currentTrackId = intent.getIntExtra(EXTRA_CURRENT_TRACK_ID, -1);
                updateNotificationText();
            } else {
            /*Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Location Tracking")
                    .setContentText("Running in the background...")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build();

            // Android 14 (API 34) UPSIDE_DOWN_CAKE requires specifying the service type at runtime
            // Android Q, API 29+ requires specifying foreground service type at runtime.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, notification);
            }*/

                // Check if the Intent contains custom interval update instructions
                if (intent != null && intent.hasExtra("update_interval")) {
                    // User overridden via SeekBar: Disable auto-speed adjustment
                    isAutoAdjustEnabled = false;
                    long newInterval = intent.getLongExtra("update_interval", 10000);
                    long newFastestInterval = intent.getLongExtra("update_fastest_interval", 5000);

                    // Trigger the runtime change function
                    changeLocationInterval(newInterval, newFastestInterval);
                } else if (intent != null && intent.hasExtra("enable_auto")) {
                    isAutoAdjustEnabled = intent.getBooleanExtra("enable_auto", true);
                } else {
                    // First-time setup launch code
                    startForegroundWithNotification();
                    startLocationUpdates();
                }
            }
        }
        return START_STICKY;
    }

    // The dedicated function to handle updates safely at runtime
    public void changeLocationInterval(long newIntervalMillis, long newFastestIntervalMillis) {
        this.currentIntervalMillis = newIntervalMillis;
        this.currentFastestIntervalMillis = newFastestIntervalMillis;

        if (fusedLocationClient != null && locationCallback != null) {
            try {
                // 1. Remove the old update callback first to prevent dual loops
                fusedLocationClient.removeLocationUpdates(locationCallback);

                // 2. Build the fresh configuration request
                LocationRequest newRequest = new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMillis)
                        .setMinUpdateIntervalMillis(currentFastestIntervalMillis)
                        .build();

                // 3. Restart tracking immediately with the new timing
                fusedLocationClient.requestLocationUpdates(newRequest, locationCallback, Looper.getMainLooper());

            } catch (SecurityException e) {
                // Fail-safe handling for permission checks
            }
        }
    }

    private void startLocationUpdates() {
        // Configure location intervals (Adjust for battery optimization)
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMillis)
                .setMinUpdateIntervalMillis(currentFastestIntervalMillis)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            // Handle missing permissions gracefully
        }
    }

    /*private void startContinuousTracking() {
        // Configure high accuracy continuous updates
        LocationRequest backgroundLocationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMillis) // Request every 10 seconds
                .setMinUpdateIntervalMillis(currentFastestIntervalMillis)       // Fastest interval 5 seconds
                .build();

        try {
            // Request the continuous tracking updates
            fusedLocationClient.requestLocationUpdates(
                    backgroundLocationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            // Handle edge case where user revoked permissions mid-run
        }
    }*/

    private void updateNotificationText() {
        String contentText = isRecordingTracks
                ? "Recording track data to database..."
                : "Streaming active location to map UI...";

        Notification notification = new NotificationCompat.Builder(this, "LocationTrackingChannel")
                .setContentTitle("Location Tracking Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(12345, notification);
        }
    }

    private void startForegroundWithNotification() {
        // for current location and distance to map
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Continuous Location Tracking Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }

        // for continuous tracking
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking Active")
                .setContentText("Streaming active location to map UI...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(12345, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(12345, notification);
        }
    }

    @Override
    public void onDestroy() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    // A service needs an inner IBinder class to allow multiple activities to connect to it.
    public class LocalBinder extends Binder {
        public TrackingService getService() {
            // Returns this exact running instance so activities can call its public methods
            return TrackingService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}

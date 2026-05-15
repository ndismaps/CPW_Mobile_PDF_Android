package com.dnrcpw.cpwmobilepdf.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.dnrcpw.cpwmobilepdf.data.DBWayPtHandler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    // Displays list of imported pdf maps and an add more button. When an item is clicked, it loads the map.
    private ListView lv;
    private CustomAdapter myAdapter; // list of imported pdf maps
    private DBHandler dbHandler;
    private DBWayPtHandler dbWayPtHandler;
    //private String TAG = "MainActivity";
    boolean sortFlag = true;
    Toolbar toolbar;
    int selectedId;
    // TODO remove
    final int MY_PERMISSIONS_LOCATION = 0;
    private static final int FOREGROUND_REQUEST_CODE = 1001;
    private static final int BACKGROUND_REQUEST_CODE = 1002;
    private LocationUpdateReceiver locationReceiver;

    // location variables
    // TODO remove
    private FusedLocationProviderClient mFusedLocationClient;
    // TODO remove
    private LocationCallback mLocationCallback;
    double latNow, latBefore = 0.0;
    double longNow, longBefore = 0.0;
    double updateProximityDist = 160.9344; // default change in distance that triggers updating proximity .1 miles
    Spinner sortByDropdown;
    TextView sortTitle;
    // Update App
    private AppUpdateManager appUpdateManager;
    private static final int APP_UPDATE_REQUEST_CODE = 123;
    private InstallStateUpdatedListener installStateUpdatedListener;
    private boolean checkedForUpdates = false;

    // Edit Menu
    //ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // added to report non sdk APIs 12/13/21
            boolean debug = false;
            if (debug) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .penaltyDeath()
                        .build());
            }

            setContentView(R.layout.activity_main);
            setTitle(R.string.title_activity_main); // "Imported Maps"

            locationReceiver = new LocationUpdateReceiver(); // new TrackingService
            // DEBUG ***********
            //latBefore = 38.5;
            //longBefore = -105.0;


            // top menu with ... button
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            //sortView = findViewById(R.id.sortByDropDown);
            // FILL SORT BY OPTIONS
            sortByDropdown = findViewById(R.id.sortBy);
            //create an adapter to describe how the items are displayed, adapters are used in several places in android.
            // sortByItems is an array defined in res/values/strings.xml
            // width is set in res/layout/spinner_dropdown_item.xml
            ArrayAdapter<CharSequence> sortByAdapter = ArrayAdapter.createFromResource(this, R.array.sortByItems,
                    R.layout.spinner_dropdown_item);
            //set the sortBy adapter to the previously created one.
            sortByDropdown.setAdapter(sortByAdapter);
            // set on click functions: onItemSelected and nothingSelected (must have these names)
            sortByDropdown.setOnItemSelectedListener(this);
            sortFlag = true;

            // Used to keep track of user movement
            //latBefore = 0.0;
            //longBefore = 0.0;

            // FLOATING ACTION BUTTON CLICK
            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(view -> {
                Intent intent = new Intent(MainActivity.this, GetMoreActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });

            // SET UP LOCATION SERVICES 5-13-26
            if (checkLocationPermissions()) {
                startTrackingService();
            } else {
                checkAndRequestTracking();
            }

            // TODO REMOVE
            // SET UP LOCATION SERVICES
            /*AlertDialog.Builder builder;
            // Ask for location permissions
            if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) ||
                    (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)) {
                // Permission is not granted. Request the permission
                if ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) ||
                        (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION))) {
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Location Permission Needed");
                    builder.setMessage("Permission to access this device's location is needed to show your current location on the map. Please click ALLOW when asked.")
                            .setPositiveButton("OK", (dialog, id) -> {
                                // User clicked OK button. Hide dialog. Ask again
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_LOCATION);
                            }).show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_LOCATION);
                }
            }*/

            // Check if GPS is enabled
            if (!isGPSEnabled(MainActivity.this)) {
                Toast.makeText(MainActivity.this, "GPS is not enabled.", Toast.LENGTH_LONG).show();
            }
            // TODO remove
            // Check if location services are turned on
            /*if (!isLocationEnabled(MainActivity.this)) {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Notice");
                builder.setMessage("Please turn ON Location Services. This can be done in your phone's Settings. If this is not turned on your current location will not appear on the map.")
                        .setPositiveButton(R.string.ok, (dialog, id) -> {
                            // User clicked OK button. Hide dialog.
                        }).show();
            } else
                setupLocation();
            */


            // Check for updates in the Play Store https://www.section.io/engineering-education/android-application-in-app-update-using-android-studio/
            appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());//this);
            installStateUpdatedListener = state -> {
                if (state.installStatus() == InstallStatus.DOWNLOADING) {
                    float bytesDownloaded = (float) state.bytesDownloaded();
                    float totalBytesToDownload = (float) state.totalBytesToDownload();
                    // Implement progress
                    Toast.makeText(getApplicationContext(),"Downloading update " + String.format(Locale.US, "%.0f", bytesDownloaded / totalBytesToDownload * 100f) + "%",Toast.LENGTH_SHORT).show();
                } else if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    // After the update is downloaded, show a notification
                    // and request user confirmation to restart the app.
                    popupForCompleteUpdate();
                } else if (state.installStatus() == InstallStatus.INSTALLED) {
                    removeInstallStateUpdateListener();
                } else {
                    Toast.makeText(getApplicationContext(), "InstallStateUpdatedListener: state: " + state.installStatus(), Toast.LENGTH_LONG).show();
                }
            };
        }catch (Exception e) {
            Log.e("Main", e.getMessage());
        }
    }

    // Setup Tracking Service 5-13-26
    private boolean checkLocationPermissions() {
        // TODO may need to add this to prevent the system from killing your tracking service to save power: Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Manifest.permission.WAKE_LOCK
        int fineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int backgroundLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            return fineLocation == PackageManager.PERMISSION_GRANTED && backgroundLocation == PackageManager.PERMISSION_GRANTED;
        }
        return fineLocation == PackageManager.PERMISSION_GRANTED;
    }


    // 1. Trigger the flow
    private void checkAndRequestTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Foreground is good! Now verify background status
            checkBackgroundAccess();
        } else {
            // Step 1: Force foreground request first
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    FOREGROUND_REQUEST_CODE);
        }
    }

    // 2. Validate background state sequentially
    private void checkBackgroundAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startTrackingService();
            } else {
                // Step 2: Educate user before opening system settings
                showBackgroundRationaleDialog();
            }
        } else {
            // Older versions automatically grant background access if foreground is active
            startTrackingService();
        }
    }

    private void showBackgroundRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Background Location Required")
                .setMessage("This app maps routes while your screen is off. Please select 'Allow all the time' on the next Settings screen under: Permissions Location, Allowed Location, to enable background logging.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
                        // System API blocks direct popups. You must open app details settings.
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    } else {
                        // Android 10 supports a targeted system dialog popup
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                BACKGROUND_REQUEST_CODE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FOREGROUND_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Foreground permission secured, proceed seamlessly to Step 2
                checkBackgroundAccess();
            }
        } else if (requestCode == BACKGROUND_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrackingService();
            }
        }
    }

    /*private void requestLocationPermissions() {
        //TODO may need to add this to prevent the system from killing your tracking service to save power: Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Manifest.permission.WAKE_LOCK
        // For Android 11+, you should ideally request Foreground first, then Background.
        // *****If you add move permissions, update ****
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }*/


    private void startTrackingService() {
        Intent intent = new Intent(this, TrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    // Define the receiver class
    private class LocationUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "ACTION_LOCATION_UPDATE".equals(intent.getAction())) {
                latNow = intent.getDoubleExtra("extra_latitude", 0.0);
                longNow = intent.getDoubleExtra("extra_longitude", 0.0);
                float accuracy = intent.getFloatExtra("extra_accuracy", 0.0f); // Read accuracy

                try {
                        // Update UI with location data
                        float[] results = new float[1];
                        //latNow = location.getLatitude();
                        //longNow = location.getLongitude(); // make it positive

                        // for debugging ****************
                        //latBefore = latBefore + .5;
                        //longBefore = longBefore -.2;

                        if (myAdapter == null) return;
                        myAdapter.setLocation(latNow,longNow);
                        //bearing = location.getBearing(); // 0-360 degrees 0 at North

                        // if accuracy is worse than 1/10 of a mile do not update distance to map

                        //Log.d("Accuracy", "onLocationResult: accuracy="+accuracy);
                        if (accuracy > 160.9344) {
                            Toast.makeText(MainActivity.this, "Acquiring location...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (latBefore != 0.0) {
                            try {
                                Location.distanceBetween(latBefore, longBefore, latNow, longNow, results);
                            } catch (IllegalArgumentException e) {
                                return;
                            }
                        }

                        // if change in location is > .1 miles update distance to map
                        if (latBefore == 0.0 || results[0] > updateProximityDist) {
                            // Update distance to map.
                            myAdapter.getDistToMap();

                            String sort = dbHandler.getMapSort();
                            if ((sort.equals("proximity") || sort.equals("proximityrev")) && sortFlag) {
                                if (sort.equals("proximity"))
                                    myAdapter.SortByProximity();
                                else
                                    myAdapter.SortByProximityReverse();
                                myAdapter.notifyDataSetChanged();

                                // Refresh all data in visible table cells
                                for (int i = 0; i < myAdapter.pdfMaps.size(); i++) {
                                    View v = lv.getChildAt(i - lv.getFirstVisiblePosition());
                                    if (v == null)
                                        continue;

                                    ImageView img = v.findViewById(R.id.pdfImage);
                                    try {
                                        File imgFile = new File(myAdapter.pdfMaps.get(i - lv.getFirstVisiblePosition()).getThumbnail());
                                        Bitmap myBitmap;
                                        myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                        if (myBitmap != null)
                                            img.setImageBitmap(myBitmap);
                                        else
                                            img.setImageResource(R.drawable.pdf_icon);
                                    } catch (Exception ex) {
                                        Toast.makeText(MainActivity.this, "Problem reading thumbnail.", Toast.LENGTH_LONG).show();
                                        img.setImageResource(R.drawable.pdf_icon);
                                    }

                                    TextView name = v.findViewById(R.id.nameTxt);
                                    name.setText(myAdapter.pdfMaps.get(i - lv.getFirstVisiblePosition()).getName());
                                    TextView fileSize = v.findViewById(R.id.fileSizeTxt);
                                    fileSize.setText(myAdapter.pdfMaps.get(i).getFileSize());
                                    TextView distToMap = v.findViewById(R.id.distToMapTxt);
                                    String dist = myAdapter.pdfMaps.get(i - lv.getFirstVisiblePosition()).getDistToMap();
                                    if (dist.equals("onmap")) {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.VISIBLE);
                                        distToMap.setText("");
                                    } else {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.GONE);
                                        distToMap.setText(dist);
                                    }
                                }
                            }
                            // Refresh only dist to map
                            else if (sortFlag) {
                                // Refresh visible table cells
                                for (int i = 0; i < myAdapter.pdfMaps.size(); i++) {
                                    View v = lv.getChildAt(i - lv.getFirstVisiblePosition());
                                    if (v == null)
                                        continue;
                                    TextView distToMap = v.findViewById(R.id.distToMapTxt);
                                    String dist = myAdapter.pdfMaps.get(i).getDistToMap();
                                    //Log.d("Distance", "accuracy:"+accuracy+"  "+myAdapter.pdfMaps.get(i).getName()+" "+dist);
                                    if (dist.equals("onmap")) {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.VISIBLE);
                                        distToMap.setText("");
                                    } else {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.GONE);
                                        distToMap.setText(dist);
                                    }
                                }
                            }
                        }

                        // save current location so we can see how much they moved
                        latBefore = latNow;
                        longBefore = longNow;

                } catch (SQLException e){
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.problemReadingDatabase) + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                // try to keep app from crashing no gps 6-15-22
                catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED)
                startTrackingService();
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrackingService();
            }else {
                // permission denied (this is the first time "never ask again" is not checked)
                // so ask again explaining the usage of permission
                // shouldShowRequestPermissionRationale will return true
                AlertDialog.Builder builder;
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Location Permission Needed");
                    builder.setMessage("This app will not run without permission to access this device's location. Please click 'ALLOW' when asked.")
                            .setPositiveButton("OK", (dialog, id) -> {
                                // User clicked OK button. Hide dialog. Ask again
                                dialog.dismiss();
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton("No, Exit App", (dialogInterface, i) -> {
                                finishAndRemoveTask();
                            }).create().show();
                }
                else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Background Location Permission Needed");
                    builder.setMessage("This app will not run without permission to access this device's location while in the background. Please click 'ALLOW all the time' when asked.")
                            .setPositiveButton("OK", (dialog, id) -> {
                                // User clicked OK button. Hide dialog. Ask again
                                dialog.dismiss();
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton("No, Exit App", (dialogInterface, i) -> {
                                finishAndRemoveTask();
                            }).create().show();
                }
                // TODO may need to add more if checks for these permissions: to prevent the system from killing your tracking service to save power: Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Manifest.permission.WAKE_LOCK
                // permission is denied (and never ask again is checked) go to Settings or exit
                // shouldShowRequestPermissionRationale will return false
                else {
                    // Ask user to go to setting and manually allow permissions
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Permissions Needed");
                    builder.setMessage("You have denied some needed permissions. Go to Settings, click on Permissions and allow all permissions all the time. Then restart this app.")
                            .setPositiveButton("Yes, Go to Settings", (dialog, id) -> {
                                // User clicked OK button. Hide dialog. Ask again
                                dialog.dismiss();
                                // Go to app settings
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finishAndRemoveTask();
                            })
                            .setNegativeButton("No, Exit App", (dialog, i) -> {
                                dialog.dismiss();
                                finishAndRemoveTask();
                            }).create().show();
                }
            }
        }
    }*/
    // END NEW CODE 5-13-26 **********************

    // Check for Updates in the Play Store
    protected void checkForUpdate(){
        // Check for app update
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED){
                popupForCompleteUpdate();
            }
            else if (!checkedForUpdates && appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    // For flexable update type need to install listener for download and prompt to restart
                    // See: https://developer.android.com/guide/playcore/in-app-updates/kotlin-java#java
                    startUpdateFlow(appUpdateInfo);
            }
        });
    }
    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        try {
            /*appUpdateManager.startUpdateFlowForResult(appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    APP_UPDATE_REQUEST_CODE);*/
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo,
                    this,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                    APP_UPDATE_REQUEST_CODE);

        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }
    // Update Downloaded? Displays the dialog notification and call to action.
    private void popupForCompleteUpdate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Update Downloaded");
        builder.setMessage("Please restart the app for these changes to take affect.")
                .setPositiveButton("RESTART", (dialog, id) -> {
                    // User clicked Restart button.
                    dialog.dismiss();
                    if (appUpdateManager != null) {
                        appUpdateManager.completeUpdate();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Please restart the app now", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("CANCEL", (dialog, i) -> dialog.dismiss()).create().show();
    }
    private void removeInstallStateUpdateListener() {
        if (appUpdateManager != null) {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                checkedForUpdates = true;
                Toast.makeText(getApplicationContext(), "Update Canceled", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(),"Downloading Update...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Update Failed!", Toast.LENGTH_LONG).show();
                if(!checkedForUpdates){
                    checkForUpdate();
                }
            }
        }
    }

    // TODO remove
   /*protected void setupLocation(){
        try {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        } catch (Exception e){
            // no gps service
            return;
        }
        // UPDATE CURRENT POSITION
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                try {
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data
                        float[] results = new float[1];
                        latNow = location.getLatitude();
                        longNow = location.getLongitude(); // make it positive

                        // for debugging ****************
                        //latBefore = latBefore + .5;
                        //longBefore = longBefore -.2;

                        if (myAdapter == null) return;
                        myAdapter.setLocation(latNow,longNow);
                        //bearing = location.getBearing(); // 0-360 degrees 0 at North

                        // if accuracy is worse than 1/10 of a mile do not update distance to map
                        float accuracy = location.getAccuracy();
                        //Log.d("Accuracy", "onLocationResult: accuracy="+accuracy);
                        if (accuracy > 160.9344) {
                            Toast.makeText(MainActivity.this, "Acquiring location...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (latBefore != 0.0) {
                            try {
                                Location.distanceBetween(latBefore, longBefore, latNow, longNow, results);
                            } catch (IllegalArgumentException e) {
                                return;
                            }
                        }

                        // if change in location is > .1 miles update distance to map
                        if (latBefore == 0.0 || results[0] > updateProximityDist) {
                            // Update distance to map.
                            myAdapter.getDistToMap();

                            String sort = dbHandler.getMapSort();
                            if ((sort.equals("proximity") || sort.equals("proximityrev")) && sortFlag) {
                                if (sort.equals("proximity"))
                                    myAdapter.SortByProximity();
                                else
                                    myAdapter.SortByProximityReverse();
                                myAdapter.notifyDataSetChanged();

                                // Refresh all data in visible table cells
                                for (int i = 0; i < myAdapter.pdfMaps.size(); i++) {
                                    View v = lv.getChildAt(i - lv.getFirstVisiblePosition());
                                    if (v == null)
                                        continue;

                                    ImageView img = v.findViewById(R.id.pdfImage);
                                    try {
                                        File imgFile = new File(myAdapter.pdfMaps.get(i - lv.getFirstVisiblePosition()).getThumbnail());
                                        Bitmap myBitmap;
                                        myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                        if (myBitmap != null)
                                            img.setImageBitmap(myBitmap);
                                        else
                                            img.setImageResource(R.drawable.pdf_icon);
                                    } catch (Exception ex) {
                                        Toast.makeText(MainActivity.this, "Problem reading thumbnail.", Toast.LENGTH_LONG).show();
                                        img.setImageResource(R.drawable.pdf_icon);
                                    }

                                    TextView name = v.findViewById(R.id.nameTxt);
                                    name.setText(myAdapter.pdfMaps.get(i - lv.getFirstVisiblePosition()).getName());
                                    TextView fileSize = v.findViewById(R.id.fileSizeTxt);
                                    fileSize.setText(myAdapter.pdfMaps.get(i).getFileSize());
                                    TextView distToMap = v.findViewById(R.id.distToMapTxt);
                                    String dist = myAdapter.pdfMaps.get(i - lv.getFirstVisiblePosition()).getDistToMap();
                                    if (dist.equals("onmap")) {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.VISIBLE);
                                        distToMap.setText("");
                                    } else {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.GONE);
                                        distToMap.setText(dist);
                                    }
                                }
                            }
                            // Refresh only dist to map
                            else if (sortFlag) {
                                // Refresh visible table cells
                                for (int i = 0; i < myAdapter.pdfMaps.size(); i++) {
                                    View v = lv.getChildAt(i - lv.getFirstVisiblePosition());
                                    if (v == null)
                                        continue;
                                    TextView distToMap = v.findViewById(R.id.distToMapTxt);
                                    String dist = myAdapter.pdfMaps.get(i).getDistToMap();
                                    //Log.d("Distance", "accuracy:"+accuracy+"  "+myAdapter.pdfMaps.get(i).getName()+" "+dist);
                                    if (dist.equals("onmap")) {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.VISIBLE);
                                        distToMap.setText("");
                                    } else {
                                        v.findViewById(R.id.locationIcon).setVisibility(View.GONE);
                                        distToMap.setText(dist);
                                    }
                                }
                            }
                        }

                        // save current location so we can see how much they moved
                        latBefore = latNow;
                        longBefore = longNow;
                    }
                } catch (SQLException e){
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.problemReadingDatabase) + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                // try to keep app from crashing no gps 6-15-22
                catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
    }*/

    public boolean isGPSEnabled(Context context){
        // 6-15-22 Check if GPS is enabled
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(IllegalArgumentException ex){
            return true;
        }
    }

    // PERMISSIONS
    // TODO remove
    // location service enabled?
    public boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            // This is Deprecated in API 28
            int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (locationMode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }

    // TODO remove
    // Permission
    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AlertDialog.Builder builder;
        if (grantResults.length == 0)return;
        if (requestCode == MY_PERMISSIONS_LOCATION) {// If request is cancelled, the result arrays are empty.
            //if (grantResults.length > 0
            //    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted
            //Toast.makeText(this,"Permission granted to access location.", Toast.LENGTH_LONG).show();
            //}
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // permission denied (this is the first time "never ask again" is not checked)
                // so ask again explaining the usage of permission
                // shouldShowRequestPermissionRationale will return true
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Location Permission Needed");
                    builder.setMessage("This app will not run without permission to access this device's location. Please click ALLOW when asked.")
                        .setPositiveButton("OK", (dialog, id) -> {
                            // User clicked OK button. Hide dialog. Ask again
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    MY_PERMISSIONS_LOCATION);
                        })
                        .setNegativeButton("No, Exit App", (dialogInterface, i) -> {
                            finishAndRemoveTask();
                        }).create().show();
                }
                // permission is denied (and never ask again is checked) go to Settings or exit
                // shouldShowRequestPermissionRationale will return false
                else {
                    // Ask user to go to setting and manually allow permissions
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Permissions Needed");
                    builder.setMessage("You have denied some needed permissions. Go to Settings, click on Permissions and allow all permissions.  Then restart this app.")
                        .setPositiveButton("Yes, Go to Settings", (dialog, id) -> {
                            // User clicked OK button. Hide dialog. Ask again
                            dialog.dismiss();
                            // Go to app settings
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", getPackageName(), null));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finishAndRemoveTask();
                        })
                        .setNegativeButton("No, Exit App", (dialog, i) -> {
                            dialog.dismiss();
                            finishAndRemoveTask();
                        }).create().show();
                }
            }
            else
                setupLocation();
        }
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        try {
            try {
                dbHandler = new DBHandler(MainActivity.this);
                dbWayPtHandler = new DBWayPtHandler(MainActivity.this);
            } catch (SQLException e){
                Toast.makeText(MainActivity.this,getResources().getString(R.string.problemReadingDatabase),Toast.LENGTH_LONG).show();
            }
            // Importing a Map hides this button, show it again
            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setVisibility(View.VISIBLE);
            latBefore = 0.0; //reset location so it updates
            fillList(); // get dbHandler and maps list from database

            // Start Location Services Receiver
            // Register receiver when UI is visible
            IntentFilter filter = new IntentFilter("ACTION_LOCATION_UPDATE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ requires specifying export flags for security
                registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(locationReceiver, filter);
            }
            // TODO remove
            /*if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                startLocationUpdates();
            } else
                Toast.makeText(MainActivity.this, "Please turn on Location Services.", Toast.LENGTH_LONG).show();
             */

            // Checks that the update is not stalled
            if (appUpdateManager != null) {
                appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                appUpdateInfo -> {
                    if (appUpdateInfo.updateAvailability()
                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // If an in-app update is already running, resume the update.
                        try {
                            /*appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    AppUpdateType.FLEXIBLE,
                                    MainActivity.this,
                                    APP_UPDATE_REQUEST_CODE);*/
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    MainActivity.this,
                                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                                    APP_UPDATE_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            Toast.makeText(getApplicationContext(), "Failed to update. " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            // e.printStackTrace();

                        }
                    } else // If the update is downloaded but not installed,
                        // notify the user to complete the update.
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            popupForCompleteUpdate();
                        }
                });
            }
        } catch(Exception tr) {
            Log.e("Main",tr.getMessage());
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        try {
            //Toast.makeText(MainActivity.this, "onPause", Toast.LENGTH_SHORT).show();

            // Location Service
            // Unregister to prevent memory leaks when app is in background
            unregisterReceiver(locationReceiver);
            // TODO remove
            /*  if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                stopLocationUpdates();
            }*/

            dbHandler.close();
            dbWayPtHandler.close();
        } catch(Exception tr) {
            Log.e("Main",tr.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }


    //--------------------
    // Database Calls
    //--------------------
    private void fillList() {
        // GET THE LIST FROM THE DATABASE
        try {
            /* force this file to push to master2 */
            myAdapter = new CustomAdapter(MainActivity.this, dbHandler.getAllMaps(), dbHandler, dbWayPtHandler);
        } catch (SQLException | NullPointerException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Error");
            if (e.getMessage() != null)
                builder.setMessage("Cannot read your maps from the disk. Is the disk full? Are too many apps running? Error message: "+e.getMessage());
            else
                builder.setMessage("Cannot read your maps from the disk. Is the disk full? Are too many apps running?");
            builder.setPositiveButton("CLOSE APP", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // send email TODO

                    // Close the app
                    System.exit(1);
                }
            }).show();
            myAdapter = new CustomAdapter(MainActivity.this, new ArrayList<>(), dbHandler, dbWayPtHandler);
        }

        lv = findViewById(R.id.lv);
        lv.setAdapter(myAdapter);

        // Make sure all the maps in the database still exist
        myAdapter.checkIfExists();

        /*lv.setLongClickable(true);
        //registerForContextMenu(lv); // set up edit/trash context menu
        lv.setChoiceMode(lv.CHOICE_MODE_MULTIPLE);
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            // Called when the user long-clicks on someView
            @Override
            public boolean onItemLongClick(AdapterView<?> view, View row,
                                           int position, long id) {
                view.setActivated(true);
                if (mActionMode != null) {
                    return false;
                }

                setTitle("Editing");
                sortByDropdown.setVisibility(View.GONE);
                sortTitle.setVisibility(View.GONE);
                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = MainActivity.this.startActionMode(mActionModeCallback);
                return true;
            }
        });*/
        /*lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // clicking on an activated item unactivates it
                if (mActionMode != null) {
                    if (view.isActivated())
                        view.setActivated(false);
                    else view.setActivated(true);
                }
            }
        });*/

        // Display note if no records found
        showHideNoImportsMessage();

        // set selected sort by item
        String sort = dbHandler.getMapSort();
        int sortID = 0;
        switch (sort) {
            case "namerev":
                sortID = 1;
                break;
            case "date":
                sortID = 2;
                break;
            case "daterev":
                sortID = 3;
                break;
            case "size":
                sortID = 4;
                break;
            case "sizerev":
                sortID = 5;
                break;
            case "proximity":
                sortID = 6;
                break;
            case "proximityrev":
                sortID = 7;
                break;
        }
        sortByDropdown.setSelection(sortID, true);
        sortMaps(sort); // added 10-24-22 When returning from activity or paused, if setSelection was not changing anything it would not sort. Defaulted to date added sorting.
        // Update myAdapter list and database if import/rename/delete happened
        checkForActivityResult();
        // check if returned from another activity and change the maps list accordingly
        // When return from GetMoreActivity or EditMapNameActivity update maps list
    }

    private void checkForActivityResult() {
        // GetMoreActivity gets map to import
        // EditMapNameActivity renames or deletes a map
        // They return extras to pass back the results, update adapter and database here
        final Intent i = MainActivity.this.getIntent();
        if (i.getExtras() != null && !i.getExtras().isEmpty()) {
            // IMPORT NEW MAP INTO LIST
            // GetMoreActivity adds a record to the DBHandler database with map name of "Loading..."
            // CustomAdapter calls importMap in CustomAdapter.java
            /*if (i.getExtras().containsKey("IMPORT_MAP") && i.getExtras().containsKey("PATH")) {
                boolean import_map = i.getExtras().getBoolean("IMPORT_MAP");
                // IMPORT MAP SELECTED
                if (import_map) {
                    // read the map pdf and load the database
                    String newPath = i.getExtras().getString("PATH");
                    //PDFMap pdfMap = new PDFMap(newPath, "", "", "", null, getResources().getString(R.string.loading), "", "");

                    sortFlag = false; // hold off on sorting.
                    FloatingActionButton fab = findViewById(R.id.fab);
                    fab.setVisibility(View.GONE);
                    // Scroll down to last item. The one just added.
                    // String name = new File(i.getExtras().getString("PATH")).getName();
                    // int pos = myAdapter.findName();
                    int pos = myAdapter.getCount() - 1;
                    if (pos > -1) lv.setSelection(pos);

                    //Toast.makeText(MainActivity.this, "Map imported: "+i.getExtras().getString("PATH"), Toast.LENGTH_LONG).show();
                }
                i.removeExtra("PATH");
                i.removeExtra("IMPORT_MAP");
            }*/

            // RENAME MAP (EditMapNameActivity)
            if (i.getExtras().containsKey("RENAME") && i.getExtras().containsKey("NAME") && i.getExtras().containsKey("ID")) {
                // Renamed map, update with new name
                //myAdapter.setEditing(false);
                String name = i.getExtras().getString("NAME");
                int id = i.getExtras().getInt("ID");
                myAdapter.rename(id, name); // also updates waypts and tracks
                //Toast.makeText(MainActivity.this, "Map renamed to: " + name, Toast.LENGTH_LONG).show();
                i.removeExtra("NAME");
                i.removeExtra("ID");
                i.removeExtra("RENAME");
            }

            // DELETE MAP
            else if (i.getExtras().containsKey("DELETE") && i.getExtras().containsKey("ID")) {
                // Delete map, remove from Imported Maps list, delete from database
                //myAdapter.setEditing(false);
                selectedId = i.getExtras().getInt("ID");
                myAdapter.removeItem(selectedId); // also removes waypts and tracks
                Toast.makeText(MainActivity.this, "Map removed", Toast.LENGTH_LONG).show();
                i.removeExtra("ID");
                i.removeExtra("DELETE");
                // Display note if no records found
                showHideNoImportsMessage();
            }
            // Checked for Updates - only do this once!
            if (i.getExtras().containsKey("UPDATES")){
                checkedForUpdates = true;
            }
        }

        // check for updates or download new update complete
        if(!checkedForUpdates) {
            checkForUpdate();
        }

        // clean up waypoints, bug left old maps that no longer exist, remove these from db
        myAdapter.removeWayPtsForOldMaps();
    }


    //--------------------
    // Location Functions
    //--------------------

    //  LOCATION UPDATES
    private void startLocationUpdates() {
        try {
            LocationRequest mLocationRequest;
            if (Build.VERSION.SDK_INT >= 31){
                mLocationRequest = new LocationRequest.Builder(1000)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setWaitForAccurateLocation(false)
                        .setMinUpdateIntervalMillis(1000)
                        .setMaxUpdateDelayMillis(1000)
                        .build();
            }
            // API <= 30
            else{
                mLocationRequest = new LocationRequest();
                if (mLocationRequest != null) {
                    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    mLocationRequest.setInterval(1000); //update location every 1 seconds
                }
            }


            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                if (mFusedLocationClient != null)
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null );
            } else {
                Toast.makeText(MainActivity.this, "Fine Location Services are off.", Toast.LENGTH_LONG).show();
            }
        }
        // 6-15-22 If looper is null and this method is executed in a thread that has not called Looper.prepare().
        catch(IllegalStateException e){
            Toast.makeText(MainActivity.this, "No Location Services."+e, Toast.LENGTH_LONG).show();
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }


    // ...................
    //   Sort By DropDown
    // ...................
    private  void sortMaps(String sortBy) {
        if (myAdapter == null) return;
        switch (sortBy) {
            case "name":
                // Sort by Name
                myAdapter.SortByName();
                lv.setAdapter(myAdapter);
                try {
                    // save user sort preference in database
                    dbHandler.setMapSort("name");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "namerev":
                // Sort by Name Reverse
                myAdapter.SortByNameReverse();
                lv.setAdapter(myAdapter);
                try {
                    // save user sort preference in database
                    dbHandler.setMapSort("namerev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "date":
                // Sort by Date
                myAdapter.SortByDate();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("date");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "daterev":
                // Sort by Date Reverse
                myAdapter.SortByDateReverse();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("daterev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "size":
                // Sort by Size
                myAdapter.SortBySize();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("size");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "sizerev":
                // Sort by Size Reverse
                myAdapter.SortBySizeReverse();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("sizerev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "proximity":
                // Sort by Proximity
                //lv.getFirstVisiblePosition(); // get current top position
                myAdapter.SortByProximity();
                lv.setAdapter(myAdapter); // scrolls to the top
                try {
                    dbHandler.setMapSort("proximity");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "proximityrev":
                // Sort by Proximity Reverse
                //lv.getFirstVisiblePosition(); // get current top position
                myAdapter.SortByProximityReverse();
                lv.setAdapter(myAdapter); // scrolls to the top
                try {
                    dbHandler.setMapSort("proximityrev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            default:
                Toast.makeText(getApplicationContext(), "Sort method not found: "+sortBy, Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) throws IllegalStateException {
        // sort by dropdown required callback
        if (!sortFlag) return;
        switch (position) {
            case 0:
                // Sort by Name
                sortMaps("name");
                break;
            case 1:
                sortMaps("namerev");
                break;
            case 2:
                // Sort by Date
                sortMaps("date");
                break;
            case 3:
                sortMaps("daterev");
                break;
            case 4:
                // Sort by Size
                sortMaps("size");
                break;
            case 5:
                sortMaps("sizerev");
                break;
            case 6:
                // Sort by Proximity
                sortMaps("proximity");
                break;
            case 7:
                sortMaps("proximityrev");
                break;
            default:
                Toast.makeText(getApplicationContext(), "Sort method not found: "+position, Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Sort by dropdown required callback
        // Do not remove!!!!
    }

    // Show or Hide no maps imported message
    private void showHideNoImportsMessage(){
        TextView msg = findViewById(R.id.txtMessage);
        sortTitle = findViewById(R.id.sortTitle);
        Spinner sortBy = findViewById(R.id.sortBy);
        if (myAdapter.pdfMaps.size() == 0){
            msg.setVisibility(View.VISIBLE);
            sortTitle.setVisibility(View.GONE);
            sortBy.setVisibility(View.GONE);
        }
        else {
            msg.setVisibility(View.GONE);
            sortTitle.setVisibility(View.VISIBLE);
            sortBy.setVisibility(View.VISIBLE);
        }
    }

    // ...................
    //     ... MENU
    // ...................
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //DELETE all imported maps clicked and recreate the database
                    myAdapter.removeAll(); // also removes all waypts and tracks
                    // Disable "Delete all Imported Maps" if there aren't any maps
                    MenuItem delMapsMenuItem = toolbar.getMenu().findItem(R.id.action_deleteAll);
                    delMapsMenuItem.setVisible(myAdapter.pdfMaps.size() != 0); // setEnabled(myAdapter.pdfMaps.size() != 0);
                    // Display note if no records found
                    showHideNoImportsMessage();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //CANCEL button clicked
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Disable "Delete all Imported Maps" if there aren't any maps
        if (toolbar == null) {
            toolbar = findViewById(R.id.toolbar);
        }
        MenuItem delMapsMenuItem = toolbar.getMenu().findItem(R.id.action_deleteAll);
        if (delMapsMenuItem != null)
            delMapsMenuItem.setVisible(myAdapter.pdfMaps.size() != 0);// setEnabled(myAdapter.pdfMaps.size() != 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Found in res/menu/menu_main.xml
        int id = item.getItemId();

        // Delete all imported maps
        if (id == R.id.action_deleteAll){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete all imported maps?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL",dialogClickListener).show();
            return true;
        }
        else if (id == R.id.action_help){
            Intent intent = new Intent(MainActivity.this, HelpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    // 7-24-23 try using context menu
    // EDIT MENU
    /*private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                //case R.id.edit_map_name:
                    /*for (int i=0; i<myAdapter.pdfMaps.size();i++ ) {
                        PDFMap map = myAdapter.pdfMaps.get(i);
                        if (map.getSelected()) {
                            Log.d("edit","renaming="+map.getName()+" to "+map.getRename());
                            myAdapter.rename(map.getId(),map.getRename());
                        }

                    }*/


                //    return true;
    /*            case R.id.delete_map:
                    // display alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Delete");
                    builder.setMessage("Delete all selected maps?").setPositiveButton("DELETE", deleteDialogClickListener)
                            .setNegativeButton("CANCEL",deleteDialogClickListener).show();
                    //mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.info:
                    // display alert dialog
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                    builder2.setTitle("Info");
                    String msg = "";
                    for (int i=0; i<myAdapter.pdfMaps.size(); i++) {
                        if (myAdapter.pdfMaps.get(i).getSelected()) {
                            Toast.makeText(MainActivity.this, "Deleting "+myAdapter.pdfMaps.get(i).getName(), Toast.LENGTH_SHORT).show();
                            msg += myAdapter.pdfMaps.get(i).getRename()+" \nLat Long: ";
                            msg += myAdapter.pdfMaps.get(i).getBounds()+"\n\n";
                        }
                    }
                    builder2.setMessage(msg).setPositiveButton("OK", infoDialogClickListener)
                            .show();
                    //mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    mode.finish(); // Action picked, so close the CAB
                    return false;
            }
        }*/
        // Remove Imported Map dialog
       /* DialogInterface.OnClickListener deleteDialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //'DELETE' button clicked, remove selected maps from imported maps
                        for (int i=myAdapter.pdfMaps.size()-1; i>-1; i--) {
                            Log.d("Map "+i+" ",myAdapter.pdfMaps.get(i).getName());
                            if (myAdapter.pdfMaps.get(i).getSelected()) {
                                Toast.makeText(MainActivity.this, "Deleting "+myAdapter.pdfMaps.get(i).getName(), Toast.LENGTH_SHORT).show();
                                Log.d("DELETING ",myAdapter.pdfMaps.get(i).getName());
                                myAdapter.removeItem(myAdapter.pdfMaps.get(i).getId());
                            }
                        }
                        mActionMode.finish();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //'CANCEL' button clicked, do nothing
                        break;
                }
            }
        };*/
        // Show Info on Imported Map dialog
        /*DialogInterface.OnClickListener infoDialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //'OK' button clicked, close
                        break;
                }
            }
        };*/

        // Called when the user exits the action mode by clicking back arrow or back button
        // Rename all selected items and unselect all
        /*@Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            // unselect all rows
            for (int i=0;i<myAdapter.pdfMaps.size();i++) {
                // Save name changes. Must be done here since CustomAdapter renameTxt.setOnFocusChangeListener is called after onDestroyActionMode
                PDFMap map = myAdapter.pdfMaps.get(i);
                if (map.getSelected()) {
                    Log.d("edit","renaming="+map.getName()+" to "+map.getRename());
                    myAdapter.rename(map.getId(),map.getRename()); // make the name change permanent
                }
                myAdapter.pdfMaps.get(i).setSelected(false);
            }
            myAdapter.setEditing(false);
            myAdapter.notifyDataSetChanged();
            setTitle("Imported Maps");
            sortByDropdown.setVisibility(View.VISIBLE);
            sortTitle.setVisibility(View.VISIBLE);
        }
    };*/
}
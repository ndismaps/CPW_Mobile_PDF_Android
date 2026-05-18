package com.dnrcpw.cpwmobilepdf.activities;

import static android.graphics.Color.argb;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.ServiceConnection;
import android.database.SQLException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.dnrcpw.cpwmobilepdf.data.DBWayPtHandler;
import com.dnrcpw.cpwmobilepdf.data.DBTrackHandler;
import com.dnrcpw.cpwmobilepdf.model.PDFMap;
import com.dnrcpw.cpwmobilepdf.model.WayPt;
import com.dnrcpw.cpwmobilepdf.model.WayPts;
import com.dnrcpw.cpwmobilepdf.model.TrackSegment;
import com.dnrcpw.cpwmobilepdf.model.Track;
import com.dnrcpw.cpwmobilepdf.model.Tracks;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/* show the map */
public class PDFActivity extends AppCompatActivity implements SensorEventListener {
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(); // for database calls
    //boolean debug = true;
    PDFView pdfView;
    ArrayList<PDFMap> maps;
    PDFMap myMap;
    Menu mapMenu;
    // Color and style of current location point
    Paint cyan;
    Paint cyanLine;
    Paint blueLine;
    Paint redLine;
    Paint cyanTrans;
    Paint red;
    Paint green; // debug
    Paint purple; // debug
    Paint white;
    Paint grey;
    Paint black;
    Paint outline;
    Paint blue;
    boolean addWayPtFlag;
    boolean addTrackFlag;
    // current screen location adjusted by zoom level
    double currentLocationX; // start offscreen
    double currentLocationY;
    double latNow;
    double longNow;
    double latBefore;
    double longBefore;
    float accuracy;
    float bearing;

    double mediaBoxX1;
    double mediaBoxY1;
    double mediaBoxX2;
    double mediaBoxY2;

    double bBoxX1;
    double bBoxY1;
    double bBoxX2;
    double bBoxY2;

    double marginTop;
    double marginBottom;
    double marginLeft;
    double marginRight;

    double mediaBoxWidth;
    double mediaBoxHeight;
    double marginXworld; // marginLeft+marginRight
    double marginYworld; // marginTop+marginBottom
    // Location
    private TrackingService trackingService;
    private boolean isBound = false;
    private TextView distanceTextView;
    private PDFActivity.LocationUpdateReceiver locationReceiver;
    //private FusedLocationProviderClient mFusedLocationClient;
    //private LocationCallback mLocationCallback;

    private double lat1;
    private double long1;
    private double lat2;
    private double long2;
    private double latDiff;
    private double longDiff;

    // Screen Sensor X rotation
    int adjust; // adjust for landscape or portrait. Sensor reports north for top of portrait screen. For landscape add 90 degrees.
    private boolean landscape;
    private boolean landscapeLocked;
    private boolean portraitLocked;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer;
    private float[] mLastMagnetometer;
    private boolean mLastAccelerometerSet;
    private boolean mLastMagnetometerSet;
    private float[] mR;
    private float[] mOrientation;
    //boolean onMap = false; // used to calculate if went off map and need to load an adjacent map
    boolean adjacentMapsBtnShowing = false; // if click on the map and not the button, hide the button to display a menu of adjacent maps
    //private float mCurrentDegree = 0f;
    private WayPts wayPts;
    private String mapName;
    private DBWayPtHandler db;
    //private DBHandler db2;
    private DBTrackHandler dbTrack;
    private Boolean markCurrent;
    private Tracks tracks;
    private int currentTrackID = -1;
    private int clickedWP; // index of waypoint that was clicked on
    private int clickedTrack; // index of track that was clicked on
    private float clickTrackX;  // x,y in screen coordinates of track that was clicked on
    private float clickTrackY;
    private int adjustWP; // index of waypoint that was clicked on to adjust location (move button clicked)
    private float adjustX; // XY screen coordinate of the adjust location icon (fixed place on screen, map moves behind it)
    private float adjustY; // XY screen coordinate
    private int del_id; // the id of the selected wayPt to delete in the adjust waypoint menu
    ImageView moveIcon; // icon used to adjust the location of the waypoint
    int moveIconWidth;
    //int count = 0;//debug
    // Adjust Waypoint Menu
    ActionMode mActionMode;
    int lastClickedWP;
    private Boolean newWP; // if added a new waypoint show balloon too
    private TextPaint txtCol; // text color for waypoint balloon popup
    private int txtSize; // text size of waypoint balloon popup
    private int marg; // text margins in waypoint balloon popup
    private int boxHt; // height of waypoint balloon popup
    private int triangle_ht; // height of the triangle on the balloon popup
    private int btn_size; // width and height of waypoint balloon popup buttons edit/move/delete
    private float pin_radius; // radius of waypoint pin
    private float pin_ht; // length of pin stem from point to center of pin head (circle)
    private int startY; // bottom of the waypoint balloon popup
    private int margX; // distance on each side of waypoint to register user click
    private int margTop; // distance above waypoint to register user click
    private int margBottom; // distance below waypoint to register user click
    private int screenWidth; // Used to see if popup balloon goes off page to the left or right
   // private StaticLayout lsLayout; // arrow right in waypoint balloon popup
    private String path;
    String bounds;
    private String strBounds;
    String viewPort;
    private String strViewPort;
    String mediaBox;
    private String strMediaBox;
    private ProgressBar wait; // indeterminate progress bar
    private Boolean showAllWayPtLabels = false;
    private Boolean showAllWayPts = true;
    private Integer stateShowAllWayPts = -1;
    private Boolean showTracks = false;
    private Integer stateShowTracks = -1;
    private Boolean loadAdjacentMaps = true;
    private Boolean deleting = false;
    AtomicReference<Double> optimalPageWidth = new AtomicReference<>((double) 0);
    AtomicReference<Double> optimalPageHeight = new AtomicReference<>((double) 0);
    MenuItem wayPtMenuItem;
    MenuItem trackMenuItem;
    Double[] LatLong;
    EditText txtLatLong;
    Button menuBtn;

   //    @SuppressLint("SourceLockedOrientationActivity")

    // Define the Location ServiceConnection interface
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Cast the IBinder to our LocalBinder and get the service instance
            TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
            trackingService = binder.getService();
            isBound = true;

            // Now you can safely call any public method inside TrackingService!
            updateUIWithServiceData();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // final RelativeLayout wait; // indeterminate progress bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        distanceTextView = findViewById(R.id.distance_text_view); // display distance traveled on current track
        wait = findViewById(R.id.loadingPanel);
        wait.setVisibility(View.VISIBLE);
        latNow = -1;
        latBefore = -1;
        longBefore = -1;
        addWayPtFlag=false;
        addTrackFlag=false;
        menuBtn = findViewById(R.id.load_adjacent_maps); // adjacent map button

        // current screen location adjusted by zoom level
        currentLocationX = 0; // start offscreen
        currentLocationY = 0;

        landscape = false;
        landscapeLocked = false;
        portraitLocked = false;
        mLastAccelerometer = new float[3];
        mLastMagnetometer = new float[3];
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        mR = new float[9];
        mOrientation = new float[3];
        //mCurrentDegree = 0f;
        wayPts = null;
        mapName = "";
        markCurrent = false;
        clickedWP = -1; // index of waypoint that was clicked on
        lastClickedWP = -1;
        clickedTrack = -1; // index of track that was clicked on
        clickTrackX = -1.0f; // x,y point of track that was clicked on
        clickTrackY = -1.0f;
        adjustWP = -1; // long press on pin to adjust location
        newWP = false; // if added a new waypoint show balloon too
        txtCol = new TextPaint(); // text color for waypoint balloon popup
        txtSize = Math.round(getResources().getDimension(R.dimen.balloon_txt_size)); // text size of waypoint balloon popup ( used to be 30 pixels)
        marg = Math.round(getResources().getDimension(R.dimen.balloon_margin)); // text margins in waypoint balloon popup used to be 20pixels
        btn_size = Math.round(getResources().getDimension(R.dimen.btn_size)); // width and height of edit/move/delete buttons
        boxHt = txtSize + (marg * 2) + btn_size; // height of waypoint balloon popup (including edit/move/delete buttons)
        triangle_ht = Math.round(getResources().getDimension(R.dimen.triangle_ht));
        pin_radius = getResources().getDimension(R.dimen.pin_radius);
        pin_ht = getResources().getDimension(R.dimen.pin_height); // length of pin stem from point to center of pin head (circle)
        startY = Math.round(pin_ht + pin_radius) + 12; // bottom of the waypoint balloon popup
        margX = Math.round(getResources().getDimension(R.dimen.wayPtXmarg));
        margTop = Math.round(getResources().getDimension(R.dimen.wayPtTmarg));
        margBottom = Math.round(getResources().getDimension(R.dimen.wayPtBmarg));

        if (Build.VERSION.SDK_INT >= 28 && Build.VERSION.SDK_INT < 30) {
            screenWidth = getResources().getDisplayMetrics().widthPixels;
        } else if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics deviceWindowMetrics = getApplicationContext().getSystemService(WindowManager.class).getMaximumWindowMetrics();
            screenWidth = deviceWindowMetrics.getBounds().width();
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
        }

        // keep app from timing out and going to screen saver
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // UNPACK OUR DATA FROM INTENT
        path = null;
        bounds = null;
        viewPort = null;
        mediaBox = null;

        Intent i = this.getIntent();
        //  GET LAT LONG BOUNDS, CONVERT FROM STRING "LAT1 LONG1 LAT2 LONG1 LAT2 LONG2 LAT1 LONG2" TO FLOATS
        try {
            // Check that values were passed
            if (i.getExtras() == null) {
                Toast.makeText(PDFActivity.this, "Can't display map, no map specifications were found.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            path = i.getExtras().getString("PATH");

            // Display Map Name
            mapName = i.getExtras().getString("NAME");
            this.setTitle(mapName);

            // GET LAT/LONG
            try {
                bounds = i.getExtras().getString("BOUNDS"); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
            }catch (NullPointerException e){
                Toast.makeText(PDFActivity.this,"Could not read page lat/long.",Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            assert bounds != null;
            getBoundsVariables();
        } catch (AssertionError | Exception ae){
            Toast.makeText(PDFActivity.this, "Trouble reading lat/long from Geo PDF. Read: " + bounds, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            // GET MEDIA BOX or PAGE BOUNDARIES for example: "0 0 612 792"
            try {
                mediaBox = Objects.requireNonNull(i.getExtras()).getString("MEDIABOX");
            } catch (NullPointerException e) {
                Toast.makeText(PDFActivity.this, "Could not read page size.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            assert mediaBox != null;
            getMediaBoxVariables();
        } catch (AssertionError | Exception ae) {
            Toast.makeText(PDFActivity.this, "Trouble reading mediaBox page boundaries from Geo PDF. Read: " + mediaBox, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            // GET MARGINS
            // bBox x1,y1 is lower-left
            // bBox x2,y2 is upper-right
            //  - origin at top-left: BBox[23 570 768 48]
            //  - origin at bottom-left: BBox[23 48 768 570] (Melissa's file)
            //  - origin at bottom-right: BBox[768 48 23 570]
            try {
                viewPort = Objects.requireNonNull(i.getExtras()).getString("VIEWPORT");
            } catch (NullPointerException e) {
                Toast.makeText(PDFActivity.this, "Trouble reading viewport from Geo PDF.", Toast.LENGTH_LONG).show();
            }
            assert  viewPort != null;
            getViewPortVariables();

        } catch (AssertionError | Exception ae) {
            Toast.makeText(PDFActivity.this, "Trouble reading viewPort margins from Geo PDF. Read: " + viewPort, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Setup Screen Sensor for X Axis rotation (flat)
        try {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } catch (NullPointerException e) {
            Toast.makeText(PDFActivity.this, "Trouble reading phone orientation", Toast.LENGTH_LONG).show();
        }

        //PDFVIEW WILL DISPLAY OUR PDFS
        pdfView = findViewById(R.id.pdfView);
        pdfView.enableAntialiasing(true); // improve rendering a little bit on low-res screens

        // SET UP LOCATION SERVICES
        try {
            locationReceiver = new PDFActivity.LocationUpdateReceiver(); // new TrackingService
            //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        } catch (Exception e){
            // no gps service
            return;
        }

        // TODO remove
        // UPDATE CURRENT POSITION
        /*mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //Log.d("LocationCallback","updating location, refreshing waypoints");
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data

                    // save last location so we can see how much they moved
                    latBefore = latNow;
                    longBefore = longNow;


                    //GeomagneticField geoField;
                    latNow = location.getLatitude();
                    longNow = location.getLongitude();

                    // Debug make it simulate user movement to draw a track
                    if (latBefore != -1){
                        Random rand = new Random();
                        int randomInt = 1;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            randomInt = rand.nextInt(1,9);
                        }
                        if (randomInt > 7) randomInt = randomInt * -1;
                        double r = (double)randomInt / 10000.0;
                        latNow =  latBefore + r;
                        randomInt = 1;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            randomInt = rand.nextInt(1,9);
                        }
                        if (randomInt > 7) randomInt = randomInt * -1;
                        r = (double)randomInt / 10000.0;
                        longNow = longBefore + r;
                    }

                    //bearing = location.getBearing(); // 0-360 degrees 0 at North
                    accuracy = location.getAccuracy();
                    // Makes top of map (north) off
                    //geoField = new GeomagneticField(
                    //        Double.valueOf(latNow).floatValue(),
                    //        Double.valueOf(longNow).floatValue(),
                    //        Double.valueOf(location.getAltitude()).floatValue(),
                    //        System.currentTimeMillis()
                    //);
                    //bearing += geoField.getDeclination(); // Adjust for declination - difference between magnetic north and true north. Phone returns magnetic north.
                    //bearing -= 90; // Adjust by 90 degrees. Canvas needs 0 at East, this returns 0 at North
                    //if (bearing<0) bearing = 360 + bearing;

                    // debug
                    //TextView bTxt = (TextView)findViewById(R.id.debug);
                    //bTxt.setText(Float.toString(bearing)+"  adjust: "+Float.toString((geoField.getDeclination()))+ "  bear: "+Float.toString(location.getBearing()));

                    pdfView.invalidate();
                    //
                    // Load Adjacent Maps?
                    // check if we need to display load adjacent maps button because current location is within 1/4 mile of other maps
                    //

                    // OLD WAY when current location goes off edge. Problem USGS and FS maps have a margin CPW maps do not. Show when close to edge.
                    //double percentX = 0.13;
                    //double percentY = 0.10;
                    //******************************
                    // DEBUG force current location
                    //******************************
                    //latNow = lat2 - latDiff*percentY;
                    //longNow = long2 - longDiff*percentX;
                    //latNow = lat1 + latDiff*percentY;
                    //longNow = long1 + longDiff*percentX;
                    //if (loadAdjacentMaps && onMap && (latNow < (lat1 + latDiff*percentX)  || latNow > (lat2 - latDiff*percentX)  || longNow < (long1 + longDiff*percentY) || longNow > (long2 - longDiff*percentY))){

                    double quarterMileInDegrees = 0.00458; // 1 degree = 54.6 miles
                    if (loadAdjacentMaps &&
                        latNow > (lat1 - quarterMileInDegrees) &&
                        latNow < (lat2 + quarterMileInDegrees) &&
                        longNow > (long1 - quarterMileInDegrees) &&
                        longNow < (long2 + quarterMileInDegrees)){
                        // Get list of all available maps and see if the current location is on or within a 1/4 mile of one or more of them
                        ArrayList<Integer> mapIds = new ArrayList<>();// PDF maps that the current location is on
                        if (maps == null) return;
                        for (int i = 0; i < maps.size(); i++) {
                            PDFMap map = maps.get(i);
                            if (map.getName().equals(mapName)) continue; // don't list current map
                            String bounds = map.getBounds(); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
                            if (bounds == null || bounds.isEmpty())
                                return; // it will be 0 length if it is importing
                            bounds = bounds.trim(); // remove leading and trailing spaces

                            // Get Latitude, Longitude bounds.
                            // aLat1 and aLong1 are the smallest values SW corner
                            // aLat2 and aLong2 are the largest NE corner
                            String[] arrLatLong = bounds.split(" ");
                            // convert strings to double
                            Double[] LatLong = new Double[arrLatLong.length];
                            for (int l = 0; l < arrLatLong.length; l++) {
                                LatLong[l] = Double.parseDouble(arrLatLong[l]);
                            }
                            // Find the smallest and largest values
                            double aLat1 = LatLong[0];
                            double aLong1 = LatLong[1];
                            double aLat2 = LatLong[0];
                            double aLong2 = LatLong[1];
                            for (int l = 0; l < LatLong.length; l = l + 2) {
                                if (LatLong[l] < aLat1) aLat1 = LatLong[l];
                                if (LatLong[l] > aLat2) aLat2 = LatLong[l];
                                if (LatLong[l + 1] < aLong1) aLong1 = LatLong[l + 1];
                                if (LatLong[l + 1] > aLong2) aLong2 = LatLong[l + 1];
                            }

                            // Is current location on this map? Add it to mapIds (array of maps that contain the current location)
                            // On map
                            if (latNow >= aLat1 && latNow <= aLat2 && longNow >= aLong1 && longNow <= aLong2) {
                                mapIds.add(i);
                            }
                        }
                        //if (mapIds.size()==0){
                            //Toast.makeText(PDFActivity.this,"No adjacent maps found to load.",Toast.LENGTH_SHORT).show();
                        //}
                        //else if (mapIds.size()==1){
                            // Only one map found that contains the current location. Load it.
                            //loadNewMap(maps, mapIds.get(0));
                            //Toast.makeText(PDFActivity.this,"Now showing adjacent map.",Toast.LENGTH_SHORT).show();
                        //}
                        if (!mapIds.isEmpty()) {
                            // Several maps found. Display button and menu to load new map.
                            //Toast.makeText(PDFActivity.this,"Several adjacent maps are available",Toast.LENGTH_SHORT).show();
                            menuBtn.setVisibility(View.VISIBLE);
                            adjacentMapsBtnShowing = true; // if they don't click on the button but click elsewhere, use this to hide the menuBtn in pdfView tap event.
                            menuBtn.setOnClickListener(view -> {
                                PopupMenu popup = new PopupMenu(PDFActivity.this, menuBtn);
                                popup.getMenuInflater().inflate(R.menu.adjacent_maps_menu, popup.getMenu());
                                for (int j = 0; j < mapIds.size(); j++) {
                                    // add(groupId, itemId, order, title) Pass the index into maps array as the itemId
                                    popup.getMenu().add(1, mapIds.get(j), j + 1, maps.get(mapIds.get(j)).getName());
                                }

                                popup.show();
                                popup.setOnMenuItemClickListener(item -> {
                                    // load the user selected map
                                    int i1 = item.getItemId();
                                    loadNewMap(maps, i1);
                                    return true;
                                });
                                // hide the Load Adjacent Maps button
                                popup.setOnDismissListener(menu -> {
                                    Button menuBtn = findViewById(R.id.load_adjacent_maps);
                                    menuBtn.setVisibility(View.GONE);
                                    adjacentMapsBtnShowing = false;
                                });
                            });
                        }
                    }
                    else{
                        menuBtn.setVisibility(View.GONE);
                        adjacentMapsBtnShowing = false;
                    }
                }
            }
        };*/

        setupColorsMoveIcon();
        setupPDFView();
    }

    // LOCATION SERVICE FUNCTIONS 5-14-26
    private void updateTrackingSpeed(long intervalMs, long fastestIntervalMs) {
        Intent intent = new Intent(this, TrackingService.class);
        intent.putExtra("update_interval", intervalMs);
        intent.putExtra("update_fastest_interval", fastestIntervalMs);

        // This tells the OS to pass the new instructions to the existing running instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }


    private void loadNewMap(ArrayList<PDFMap> maps, int id){
        if (id > maps.size()-1)return;
        path = maps.get(id).getPath();
        mapName = maps.get(id).getName();
        PDFActivity.this.setTitle(mapName);
        bounds = maps.get(id).getBounds();
        getBoundsVariables();
        mediaBox = maps.get(id).getMediabox();
        getMediaBoxVariables();
        viewPort = maps.get(id).getViewport();
        getViewPortVariables();
        wayPts = db.getWayPts(mapName);
        wayPts.SortPts();
        try {
            tracks = dbTrack.getTracks(mapName);
        }catch (SQLException exc){
            Toast.makeText(PDFActivity.this, "Failed to read tracks from database. "+exc.getMessage(), Toast.LENGTH_LONG).show();
        }catch (Exception exc){
            Toast.makeText(PDFActivity.this, "Failed to read tracks from database. "+exc.getMessage(), Toast.LENGTH_LONG).show();
        }
        clickedWP = -1; // hide balloon
        clickedTrack = -1; // hide balloon for tracks
        clickTrackX = -1.0f;
        clickTrackY = -1.0f;
        // set orientation for this map
        portraitLocked = maps.get(id).getMapOrientation().equals("portrait");
        landscapeLocked = maps.get(id).getMapOrientation().equals("landscape");
        action_landscape.setChecked(landscapeLocked);
        action_portrait.setChecked(portraitLocked);
        setupPDFView();
    }
    private void getBoundsVariables(){
        bounds = bounds.trim(); // remove leading and trailing spaces
        strBounds = bounds;

        //Toast.makeText(PDFActivity.this, bounds, Toast.LENGTH_LONG).show();
        // Get Latitude, Longitude bounds.
        // lat1 and long1 are the smallest values SW corner
        // lat2 and long2 are the largest NE corner
        String[] arrLatLong = bounds.split(" ");
        // convert strings to double
        LatLong = new Double[arrLatLong.length];
        for (int l=0; l<arrLatLong.length; l++) {
            LatLong[l] = Double.parseDouble(arrLatLong[l]);
        }
        // Find the smallest and largest values
        lat1 = LatLong[0];
        long1 = LatLong[1];
        lat2 = LatLong[0];
        long2 = LatLong[1];
        int l;
        for (l=0; l<LatLong.length; l=l+2) {
            if (LatLong[l] < lat1) lat1 = LatLong[l];
            if (LatLong[l] > lat2) lat2 = LatLong[l];
            if (LatLong[l+1] < long1) long1 = LatLong[l+1];
            if (LatLong[l+1] > long2) long2 = LatLong[l+1];
        }
        // check if not a perfect rectangle
        // Use the average of the 2 points
        // get the lat/long value that is closest and use the average
        if (Double.compare(LatLong[0],LatLong[2]) != 0 && Double.compare(LatLong[0],LatLong[4]) != 0 && Double.compare(LatLong[0],LatLong[6]) != 0){
            // the lat long that is close to lat1, lat2, long1, long2
            double lat1a, lat2a, long1a, long2a;
            lat1a = lat1;
            lat2a = lat2;
            long1a = long1;
            long2a = long2;
            double smLat1Diff = 1000;
            double smLat2Diff = 1000;
            double smLong1Diff = 1000;
            double smLong2Diff = 1000;
            for (l=0; l <LatLong.length; l=l+2){
                if (Math.abs(lat1 - LatLong[l]) != 0 && Math.abs(lat1 - LatLong[l]) < smLat1Diff ){
                    smLat1Diff = Math.abs(lat1 - LatLong[l]);
                    lat1a = LatLong[l];
                }
                if (Math.abs(lat2 - LatLong[l]) != 0 && Math.abs(lat2 - LatLong[l]) < smLat2Diff ){
                    smLat2Diff = Math.abs(lat2 - LatLong[l]);
                    lat2a = LatLong[l];
                }
                if (Math.abs(long1 - LatLong[l+1]) != 0 && Math.abs(long1 - LatLong[l+1]) < smLong1Diff ){
                    smLong1Diff = Math.abs(long1 - LatLong[l+1]);
                    long1a = LatLong[l+1];
                }
                if (Math.abs(long2 - LatLong[l+1]) != 0 && Math.abs(long2 - LatLong[l+1]) < smLong2Diff ){
                    smLong2Diff = Math.abs(long2 - LatLong[l+1]);
                    long2a = LatLong[l+1];
                }
            }
            // Average
            lat1 = (lat1 + lat1a) / 2;
            lat2 = (lat2 + lat2a) / 2;
            long1 = (long1 + long1a) / 2;
            long2 = (long2 + long2a) / 2;
        }

        longDiff = long2 - long1;
        latDiff = lat2 - lat1;
    }

    private void getMediaBoxVariables(){
        mediaBox = mediaBox.trim(); // remove leading and trailing spaces
        strMediaBox = mediaBox;
        int pos = mediaBox.indexOf(" ");
        // FIND X1
        mediaBoxX1 = Double.parseDouble(mediaBox.substring(0, pos));
        // FIND Y1
        mediaBox = mediaBox.substring(pos + 1); // strip off 'X1 '
        pos = mediaBox.indexOf(" ");
        mediaBoxY1 = Double.parseDouble(mediaBox.substring(0, pos));
        // FIND X2
        mediaBox = mediaBox.substring(pos + 1); // strip off 'Y1 '
        pos = mediaBox.indexOf(" ");
        mediaBoxX2 = Double.parseDouble(mediaBox.substring(0, pos));
        // FIND Y2
        mediaBox = mediaBox.substring(pos + 1); // strip off 'X2 '
        pos = mediaBox.indexOf(" ");
        mediaBoxY2 = Double.parseDouble(mediaBox.substring(pos + 1));
    }
    private void getViewPortVariables(){
        viewPort = viewPort.trim();
        strViewPort = viewPort;
        // FIND bBoxX1
        int pos = viewPort.indexOf(" ");
        bBoxX1 = Double.parseDouble(viewPort.substring(0, pos));
        // FIND bBoxY1
        viewPort = viewPort.substring(pos + 1); // strip off 'bBoxX1 '
        pos = viewPort.indexOf(" ");
        bBoxY1 = Double.parseDouble(viewPort.substring(0, pos));
        // FIND bBoxX2
        viewPort = viewPort.substring(pos + 1); // strip off 'bBoxY1 '
        pos = viewPort.indexOf(" ");
        bBoxX2 = Double.parseDouble(viewPort.substring(0, pos));
        // FIND bBoxY2
        viewPort = viewPort.substring(pos + 1); // strip off 'bBoxX2 '
        pos = viewPort.indexOf(" ");
        bBoxY2 = Double.parseDouble(viewPort.substring(pos + 1));

        // 5-17-22 A map made by Melissa in ArcGIS had these switched
        if(bBoxY1 < bBoxY2) {
            marginTop = mediaBoxY2 - bBoxY2;
            marginBottom = bBoxY1;
        }else{
            marginTop = mediaBoxY2 - bBoxY1;
            marginBottom = bBoxY2;
        }
        if (bBoxX1 < bBoxX2){
            marginLeft = bBoxX1;
            marginRight = mediaBoxX2 - bBoxX2;
        }else{
            marginLeft = bBoxX2;
            marginRight = mediaBoxX2 - bBoxX1;
        }

        mediaBoxWidth = mediaBoxX2 - mediaBoxX1;
        mediaBoxHeight = mediaBoxY2 - mediaBoxY1;
        marginXworld = marginLeft + marginRight;
        marginYworld = marginTop + marginBottom;
    }

    private void setupColorsMoveIcon(){
        // Set color and fill of the current location point
        cyan = new Paint(Paint.ANTI_ALIAS_FLAG);
        cyanLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        cyanTrans = new Paint(Paint.ANTI_ALIAS_FLAG);
        red = new Paint(Paint.ANTI_ALIAS_FLAG);
        redLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        green = new Paint(Paint.ANTI_ALIAS_FLAG); // debug
        purple = new Paint(Paint.ANTI_ALIAS_FLAG); // debug
        white = new Paint(Paint.ANTI_ALIAS_FLAG);
        black = new Paint(Paint.ANTI_ALIAS_FLAG);
        grey = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline = new Paint();
        blue = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        cyan.setAntiAlias(true);
        cyan.setColor(Color.CYAN);
        cyan.setStyle(Paint.Style.FILL);

        float lineWidth = 12.0f;
        cyanLine.setAntiAlias(true);
        cyanLine.setColor(Color.CYAN);
        cyanLine.setStyle(Paint.Style.STROKE);
        cyanLine.setStrokeWidth(lineWidth);

        redLine.setAntiAlias(true);
        redLine.setColor(Color.RED);
        redLine.setStyle(Paint.Style.STROKE);
        redLine.setStrokeWidth(lineWidth);

        blueLine.setAntiAlias(true);
        blueLine.setColor(Color.BLUE);
        blueLine.setStyle(Paint.Style.STROKE);
        blueLine.setStrokeWidth(lineWidth);

        outline.setColor(Color.WHITE);
        outline.setStyle(Paint.Style.FILL);
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        red.setColor(Color.RED);
        red.setStyle(Paint.Style.FILL);
        grey.setColor(Color.GRAY);
        grey.setStyle(Paint.Style.FILL);
        //green.setColor(Color.GREEN); // debug
        //green.setStyle(Paint.Style.FILL); // debug
        //purple.setColor(Color.argb(100,255,0,255)); // debug
        //purple.setStyle(Paint.Style.FILL); // debug
        black.setColor(Color.BLACK);
        black.setStyle(Paint.Style.FILL);
        cyanTrans.setColor(argb(40, 0, 255, 255));
        cyanTrans.setStyle(Paint.Style.FILL);
        blue.setAntiAlias(true);
        blue.setColor(Color.BLUE);
        blue.setStyle(Paint.Style.FILL);
        // Waypoint popup text
        txtCol.setStyle(Paint.Style.FILL);
        txtCol.setColor(Color.BLACK);
        txtCol.setTextSize(txtSize);

        // add moveIcon for fine adjustment of location
        moveIcon = new ImageView(PDFActivity.this);
        moveIcon.setImageResource(R.drawable.location_search);
        moveIconWidth = Math.round(getResources().getDimension(R.dimen.btn_size));
        pdfView.addView(moveIcon,moveIconWidth,moveIconWidth);
        moveIcon.setVisibility(View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPDFView(){
        marg = Math.round(getResources().getDimension(R.dimen.ten_dp));
        float boxWidth = 3*btn_size + 2*marg;
        // GET THE PDF FILE
        File file = new File(path);
        if (file.canRead()) {
            // LOAD IT, load only first page
            //
            // LISTEN FOR TAP TO ADD WAYPOINT OR DISPLAY PT DATA
            //
            pdfView.fromFile(file).defaultPage(0).pages(0).onRender((onRenderListener) -> {
                pdfView.fitToWidth(0); // optionally pass page number
            })
                // SINGLE TAP
                .onTap(e -> {
                    // if tapped while moving or deleting a waypoint finish that
                    if (adjustWP != -1 || deleting) return false;

                    // current location went off-screen, several maps were found that contained
                    // the current location, a button was displayed to prompt the user for which
                    // one they want to load, but they clicked elsewhere so hide the button.
                    if (adjacentMapsBtnShowing){
                        menuBtn.setVisibility(View.GONE);
                        adjacentMapsBtnShowing = false;
                    }
                    updatePageSize(); // get new PDF page width and height

                    if (!showAllWayPts && !addWayPtFlag) {
                        //Toast.makeText(PDFActivity.this,"Waypoints are hidden.",Toast.LENGTH_LONG).show();
                        return false;
                    }
                    // show wait icon
                    //wait.setVisibility(View.VISIBLE);
                    boolean found = false;
                    newWP = false; // if added a new waypoint show balloon too
                    float x, y;
                    float zoom = pdfView.getZoom();
                    double toScreenCordX = (optimalPageWidth.get() * zoom) / mediaBoxWidth;
                    double toScreenCordY = (optimalPageHeight.get() * zoom) / mediaBoxHeight;
                    double marginL = toScreenCordX * marginLeft;
                    double marginT = toScreenCordY * marginTop;
                    double marginx = toScreenCordX * marginXworld;
                    double marginy = toScreenCordY * marginYworld;
                    x = (e.getX() - pdfView.getCurrentXOffset());
                    y = (e.getY() - pdfView.getCurrentYOffset());
                    double wayPtX, wayPtY;
                    //
                    // Check if clicked on waypoint popup balloon of the single waypoint that is showing the balloon
                    //
                    if (clickedWP != -1 && clickedWP < wayPts.size()) {
                        wayPtX = (((wayPts.get(clickedWP).getX() - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx)) + marginL;
                        wayPtY = (((lat2 - wayPts.get(clickedWP).getY()) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                        Boolean continueProcessing;
                        continueProcessing = checkForWaypointButtonClick(boxWidth, x, y, wayPtX, wayPtY, clickedWP);
                        if (!continueProcessing) return false;
                    }
                    //
                    // Check if clicked on track popup balloon of the single track that is showing the balloon
                    //
                    if (tracks != null && tracks.size()>0 && clickedTrack != -1 && clickedTrack < tracks.size()) {
                        Boolean continueProcessing;
                        continueProcessing = checkForWaypointButtonClick(boxWidth, x, y, clickTrackX, clickTrackY, clickedTrack);
                        if (!continueProcessing) return false;
                    }

                    // Now just show labels - do not process button clicks
                    // check if clicked on any balloon when all waypoints are showing their labels
                    /*if (showAllWayPtLabels) {
                        //Log.d("PDFActivity","Show all waypoint labels.");
                        for (int j = wayPts.size()-1; j > -1; j--) {
                            wayPtX = (((wayPts.get(j).getX() - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx)) + marginL;
                            wayPtY = (((lat2 - wayPts.get(j).getY()) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                            Boolean continueProcessing;
                            continueProcessing = checkForWaypointButtonClick(boxWidth, x, y, wayPtX, wayPtY, j);
                            if (!continueProcessing) return false;
                        }
                    }*/

                    //
                    // Check if clicked on existing waypoint
                    //
                    double longitude = (((x - marginL) / ((optimalPageWidth.get() * zoom) - marginx)) * longDiff) + long1;
                    double latitude = ((((y - marginT) / ((optimalPageHeight.get() * zoom) - marginy)) * latDiff) - lat2) * -1;
                    // If showing all balloons, and click to add new point it should add it and not hide the currently selected balloon.
                    if (showAllWayPtLabels) clickedWP = -1;

                    //TextView bTxt = (TextView)findViewById(R.id.debug);
                    //bTxt.setTextColor(Color.WHITE);
                    //bTxt.setText("X offset: "+pdfView.getCurrentXOffset()+" Tap at: " +x+", "+y+" Long: "+String.format(Locale.US,"%.4f",longitude)+ " Lat: "+String.format(Locale.US,"%.4f",latitude));

                    // If clicked on existing waypoint show balloon with name
                    int i1;
                    for (i1 = wayPts.size() - 1; i1 > -1; i1--) {
                        // convert this waypoint lat, long to screen coordinates
                        wayPtX = ((wayPts.get(i1).getX() - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                        wayPtY = (((lat2 - wayPts.get(i1).getY()) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;

                        if (x > (wayPtX - margX) && x < (wayPtX + margX) &&
                                y < (wayPtY + margBottom) && y >= (wayPtY - margTop)) {
                            lastClickedWP = clickedWP;
                            clickedWP = i1;
                            found = true;
                            pdfView.invalidate();
                            //Log.d("onTap","Clicked on existing waypoint.");
                            break;
                        }
                    }

                    // If clicked on a track show balloon
                    if (tracks != null && tracks.size()>0) {
                        clickedTrack = -1; // tracks index
                        clickTrackX = -1;
                        clickTrackY = -1;
                        for (var t = 0; t < tracks.size(); t++) {
                            Track track = tracks.get(t);

                            for (var s = 0; s < tracks.get(t).getTrackSegments().size(); s++) {
                                TrackSegment segment = track.getTrackSegments().get(s);
                                if (segment != null) {
                                    // Check if clicked point is on the line segment using the distance of a point to a line
                                    float dist = distToSegmentSquared(x, y,
                                            segment.getX1(zoom, marginx, marginL, long1, longDiff, optimalPageWidth.get()),
                                            segment.getY1(zoom, marginx, marginL, lat2, latDiff, optimalPageHeight.get()),
                                            segment.getX2(zoom, marginx, marginL, long1, longDiff, optimalPageWidth.get()),
                                            segment.getY2(zoom, marginx, marginL, lat2, latDiff, optimalPageHeight.get()));
                                    dist = (float) Math.sqrt(dist); // square root
                                    if (dist < 100)
                                        Log.d("DEBUG", "distance squared " + dist + " zoom=" + zoom);
                                    if (dist < 100) {
                                        // show popup for track
                                        clickedTrack = t; // tracks index
                                        clickTrackX = x;
                                        clickTrackY = y;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Add new waypoint
                    if (clickedWP == -1) {
                        // Check if waypoint menu item is active and set it to inactive
                        if (wayPtMenuItem != null)
                            wayPtMenuItem.setIcon(R.mipmap.ic_grey_pin_forgnd);
                        //Log.d("onTap", "map click detected (not on existing waypoint.)");
                        // Make sure user click is not off the map!
                        if (!(latitude > lat1 && latitude < lat2 && longitude > long1 && longitude < long2)) {
                            //Toast.makeText(PDFActivity.this, getString(R.string.offMap), Toast.LENGTH_LONG).show();
                            addWayPtFlag = false;
                            clickedWP = -1;
                        }
                        // Add new waypoint to database
                        else {
                            //Log.d("onTap","Add new waypoint to database.");
                            // ignore taps unless pressed add waypoint button first
                            if (!addWayPtFlag) return false;
                            wait.setVisibility(View.VISIBLE);
                            newWP = true;
                            String location = String.format(Locale.US,"%.5f, %.5f", latitude,longitude);
                            int num = findAUniqueName();
                            WayPt wayPt = new WayPt(mapName, "Waypoint " + num, (float) longitude, (float) latitude, "blue", location);
                            try {
                                db.addWayPt(wayPt);
                            } catch (SQLException exc) {
                               // wayPts.remove((float)longitude,(float)latitude);
                                Toast.makeText(PDFActivity.this, "Failed to save waypoint. "+exc.getMessage(), Toast.LENGTH_LONG).show();
                                clickedWP = -1;
                                newWP = false;
                                addWayPtFlag=false;
                                return false;
                            }
                            wayPts = db.getWayPts(mapName);
                            wayPts.SortPts();
                            // get the index of the new waypoint
                            for (i1 = 0; i1 < wayPts.size(); i1++) {
                                if (wayPts.get(i1).getX() == (float) longitude && wayPts.get(i1).getY() == (float) latitude) {
                                    lastClickedWP = clickedWP;
                                    clickedWP = i1;
                                    //Log.d("onTap","Added to database, clickedWP="+clickedWP);
                                    break;
                                }
                            }
                            // reset add waypoint button
                            addWayPtFlag=false;
                        }
                    }
                    // hide old balloon
                    else if (!found) {
                        clickedWP = -1;
                    }
                    pdfView.invalidate();
                    //Log.d("onTap", "clickedWP="+clickedWP);
                    return false;
                }).onDraw((canvas, pageWidth, pageHeight, displayedPage) -> {
                    //Log.d("onDraw", "enter onDraw pageWidth="+pageWidth+" pageHeight="+pageHeight+" zoom="+pdfView.getZoom());
                    updatePageSize(); // get new PDF page width and height
                    // Display current lat/long position
                    TextView pTxt = findViewById(R.id.cur_pos);
                    pTxt.setTextColor(Color.WHITE);
                    String str;
                    if (latNow == -1){
                        str =  getString(R.string.CurPos) + "Acquiring...";
                    }
                    else if (latNow >= lat1 && latNow <= lat2 && longNow >= long1 && longNow <= long2) {
                        str = getString(R.string.CurPos) + String.format(Locale.US,"%.05f", latNow) + ", " + String.format(Locale.US,"%.05f", longNow);
                        //onMap = true;
                    } else {
                        str = getString(R.string.CurPos) + "Not on Map";
                    }
                    pTxt.setText(str);

                    // -------------------------------------
                    //   Draw current location
                    // -------------------------------------
                    double zoom = pdfView.getZoom();
                    // convert to screen coordinates
                    double toScreenCordX = (optimalPageWidth.get() * zoom) / mediaBoxWidth;
                    double toScreenCordY = (optimalPageHeight.get() * zoom) / mediaBoxHeight;
                    double marginL = toScreenCordX * marginLeft;
                    double marginT = toScreenCordY * marginTop;
                    double marginx = toScreenCordX * marginXworld;
                    double marginy = toScreenCordY * marginYworld;

                    //if (debug ) {
                    /*    Log.d("MediaBox","0 0 "+mediaBoxX2+" "+mediaBoxY2);
                        Log.d("BBox",bBoxX1+" "+bBoxY1+" "+bBoxX2+" "+bBoxY2);
                        Log.d("Margins","Left="+marginLeft+" Right="+marginRight+" Top="+marginTop+" Bottom="+marginBottom);
                        Log.d("PageWidth",""+(optimalPageWidth.get()*zoom));
                        Log.d("Long1",""+""+long1);
                        Log.d("Long2",""+long2);
                        Log.d("longDiff",""+longDiff);
                        Log.d("marginx",""+marginx);
                        Log.d("marginL",""+marginL);
                        Log.d("PageHeight",""+(optimalPageHeight.get()*zoom));
                        Log.d("Lat1",""+""+lat1);
                        Log.d("Lat2",""+lat2);
                        Log.d("latDiff",""+latDiff);
                        Log.d("marginy",""+marginy);
                        Log.d("marginT",""+marginT);*/
                        // debug: blue dots at lat long bounds
                        //currentLocationX = (((longNow + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                        //currentLocationY = ((((90 - latNow) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;

                        //double x1,x2, y1, y2;
                        // long1, lat1
                        /*x1 = (((LatLong[1] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                         y1 = ((((90 - LatLong[0]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                        canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);
                        // long2, lat2
                        x1 = (((LatLong[3] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                        y1 = ((((90 - LatLong[2]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                        canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);
                        // long3,lat3
                        x1 = (((LatLong[5] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                        y1 = ((((90 - LatLong[4]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                        canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);
                        // long4, lat4
                        x1 = (((LatLong[7] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                        y1 = ((((90 - LatLong[6]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                        canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);*/

                        // debug:  place black dots at page width/height
                        /*canvas.translate(0, 0);
                        canvas.drawCircle(5, 0, 5f, black);
                        canvas.drawCircle((float) (optimalPageWidth.get() * zoom)-5f, (float) (optimalPageHeight.get() * zoom)-5f, 5f, black);
                        */
                        // debug: green dots at bbox Viewport
                        /*canvas.translate(0, 0);
                        x1 = toScreenCordX * bBoxX1;
                        y1 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY1;
                        canvas.drawCircle((float)x1+5f,(float)y1+5f,14f, green);
                        x2 = toScreenCordX * bBoxX2;
                        y2 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY2;
                        canvas.drawCircle((float)x2-5f,(float)y2-5f,14f, green);
                        x1 = toScreenCordX * bBoxX2;
                        y1 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY1;
                        canvas.drawCircle((float)x1-5f,(float)y1+5f,14f, green);
                        x2 = toScreenCordX * bBoxX1;
                        y2 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY2;
                        canvas.drawCircle((float)x2+5f,(float)y2-5f,14f, green);*/

                        // debug: purple dots at MediaBox
                        /*canvas.drawCircle((float)8f,(float)-8f,16f, purple);
                        x2 = toScreenCordX * mediaBoxX2;
                        y2 = toScreenCordY * mediaBoxY2;
                        canvas.drawCircle((float)x2-8f,(float)y2-8f,16f, purple);*/


                        // debug: red dots at page margins, works for NREL maps, not for USFS maps
                      /* canvas.drawCircle((float) marginL+5f, (float) marginT+5f, 10f, red);
                        // Toast.makeText(PDFActivity.this, "xRatio="+xRatio+"  left=" + x1+"  top="+y1+"  width="+pdfView.getOptimalPageWidth(), Toast.LENGTH_SHORT).show();
                        x1 = ((optimalPageWidth.get() * zoom) - (toScreenCordX * marginRight));
                        y1 = ((optimalPageHeight.get() * zoom) - (toScreenCordY * marginBottom));
                        canvas.drawCircle((float) x1-5f, (float) y1-5f, 10f, red);
                        canvas.drawCircle((float) marginL+5f, (float) y1-5f, 10f, red);
                        canvas.drawCircle((float) x1-5f, (float) marginT+5f, 10f, red);
                        */
                    //}
                    // debug lat/long at corners for Poudre Park FS
                    //x1 = (((-105.37499 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                    //y1 = ((((90 - 40.75) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                    //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);
                    //x1 = (((-105.25 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                    //y1 = ((((90 - 40.625) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                    //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);
                    //x1 = (((-105.25 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                    //y1 = ((((90 - 40.75) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                    //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);
                    //x1 = (((-105.37499 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                    //y1 = ((((90 - 40.625) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                    //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);



                    // count++;
                    // Log.d("border","zoom="+zoom);
                    //Toast.makeText(PDFActivity.this, "counter="+count+"   zoom="+zoom+"  right=" + x1+"  bottom="+y1, Toast.LENGTH_SHORT).show();

                    // Draw Tracks
                    if (tracks != null && tracks.size()>0 && showTracks) {
                        // Must store the path and redraw each segment each time
                        canvas.translate(0,0);
                        for (var t=0; t<tracks.size(); t++) {
                            Track currentTrack = tracks.get(t);
                            Paint lineColor;
                            switch (currentTrack.getColorName()) {
                                case "blue":
                                    lineColor = blueLine;
                                    break;
                                case "red":
                                    lineColor = redLine;
                                    break;
                                default:
                                    lineColor = cyanLine;
                                    break;
                            }

                            // Add to the current track's path with current location if on map
                            if (latBefore != -1 && currentTrackID != -1 && t == currentTrackID &&
                                    (latNow >= lat1 && latNow <= lat2) &&
                                    (longNow >= long1 && longNow <= long2)) {
                                currentTrack.addTrackSegment((float) longBefore, (float) latBefore, (float) longNow, (float) latNow);
                                // Save new line segment in database
                            //    dbTrack.updateTrack(currentTrack);// move to LocationUpdateReceiver
                            }
                            // Draw all tracks
                            List<TrackSegment> segments = currentTrack.getTrackSegments();
                            for (int i = 0; i < currentTrack.getTrackSegments().size(); i++) {
                                canvas.drawLine(segments.get(i).getX1(zoom, marginx, marginL, long1, longDiff, optimalPageWidth.get()),
                                        segments.get(i).getY1(zoom, marginy, marginT, lat2, latDiff, optimalPageHeight.get()),
                                        segments.get(i).getX2(zoom, marginx, marginL, long1, longDiff, optimalPageWidth.get()),
                                        segments.get(i).getY2(zoom, marginy, marginT, lat2, latDiff, optimalPageHeight.get()), lineColor);
                            }
                        }
                    }


                    // Draw the current location as a point on the map. Color of the point is defined in paint & outline above.
                    //  CONVERT LAT LONG TO SCREEN COORDINATES
                    currentLocationX = ((longNow - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                    currentLocationY = (((lat2 - latNow) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;

                    // Add waypoint at current location
                    if (markCurrent) {
                        //Log.d("PDFActivity", "onDraw: waypoint at current location");
                        markCurrent = false;
                        float theLat = (float) latNow;
                        float theLong = (float) longNow;
                        String location = String.format(Locale.US,"%.5f", theLat) + ", " + String.format(Locale.US,"%.5f", theLong);
                        int num = findAUniqueName();
                        WayPt wayPt = wayPts.add(mapName, "Waypoint " + num, theLong, theLat, "red", location);
                        wayPts.SortPts();
                        try {
                            db.addWayPt(wayPt);
                        } catch (SQLException exc) {
                            Toast.makeText(PDFActivity.this, "Failed to add pt to database.", Toast.LENGTH_LONG).show();
                        }
                        for (int i1 = 0; i1 < wayPts.size(); i1++) {
                            if (wayPts.get(i1).getX() == theLong && wayPts.get(i1).getY() == theLat) {
                                lastClickedWP = clickedWP;
                                clickedWP = i1;
                                //Log.d("onDraw","Added waypoint at current location, clickedWP="+clickedWP);
                                break;
                            }
                        }
                    }
                    // Draw Waypoints
                    if (showAllWayPts) {
                        for (int i12 = 0; i12 < wayPts.size(); i12++) {
                            //Log.d("PDFActivity","drawing waypoint "+i12);
                            double xLong = wayPts.get(i12).getX();
                            double yLat = wayPts.get(i12).getY();
                            // convert lat, long to screen coordinates
                            float x = (float) (((xLong - long1) / longDiff) * (((optimalPageWidth.get() * zoom) - marginx)) + marginL);
                            float y = (float) (((lat2 - yLat) / latDiff) * (((optimalPageHeight.get() * zoom) - marginy)) + marginT);

                            float inner_radius = getResources().getDimension(R.dimen.pin_inner_radius);
                            float pin_stem = pin_ht - pin_radius;
                            float cl_offset = getResources().getDimension(R.dimen.pin_cl_offset); // catch light offset
                            float cl_radius = getResources().getDimension(R.dimen.pin_cl_radius);
                            float pt_radius = getResources().getDimension(R.dimen.pin_pt_radius);
                            // draw outline
                            canvas.drawCircle(x, y - pin_ht, pin_radius, white); // white outline
                            Paint color = blue;
                            if (wayPts.get(i12).getColorName().equals("cyan")) color = cyan;
                            else if (wayPts.get(i12).getColorName().equals("red")) color = red;

                            // Adjusting waypoint location draw point at location for reference
                            if (adjustWP == i12){
                                color = grey;
                            }

                            // xy point oval
                            RectF pt_rect = new RectF(x - (pt_radius * 2), y - pt_radius, x + (pt_radius * 2), y + pt_radius);
                            canvas.drawOval(pt_rect, black); // point
                            canvas.drawCircle(x, y - pin_ht, inner_radius, color); // center color
                            canvas.drawCircle(x - cl_offset, y - pin_ht - cl_offset, cl_radius, white); // catch light
                            // Draw 5 pixel wide pin stem
                            canvas.drawLine(x - 3, y - pin_stem, x - 3, y, black);
                            canvas.drawLine(x - 2, y - pin_stem, x - 2, y, white);
                            canvas.drawLine(x - 1, y - pin_stem, x - 1, y, white);
                            canvas.drawLine(x, y - pin_stem, x, y, white);
                            canvas.drawLine(x + 1, y - pin_stem, x + 1, y, white);
                            canvas.drawLine(x + 2, y - pin_stem, x + 2, y, white);

                            // Show all Waypoint Labels
                            if (showAllWayPtLabels && (adjustWP != i12)) {
                                String desc = wayPts.get(i12).getDesc();
                                //drawPopup(canvas, x, y, boxWidth, desc);
                                drawLabels(canvas, x, y, boxWidth, desc);
                            }
                        }
                    }

                    // Draw popup if waypoint was clicked on
                    //if ((newWP || clickedWP != -1) && !showAllWayPtLabels && showAllWayPts && adjustWP == -1) {
                    if ((newWP || clickedWP != -1) && showAllWayPts && adjustWP == -1) {
                        //Log.d("PDFActivity", "onDraw: draw waypoint and popup balloon. newWP="+newWP+" clickedWP="+clickedWP);
                        if (clickedWP != -1 && clickedWP < wayPts.size()) {
                            int i12 = clickedWP;
                            double xLong = wayPts.get(i12).getX();
                            double yLat = wayPts.get(i12).getY();

                            // convert lat, long to screen coordinates
                            float x = (float) (((xLong - long1) / longDiff) * (((optimalPageWidth.get() * zoom) - marginx)) + marginL);
                            float y = (float) (((lat2 - yLat) / latDiff) * (((optimalPageHeight.get() * zoom) - marginy)) + marginT);

                            String desc = wayPts.get(i12).getDesc();
                            drawPopup(canvas, x, y, boxWidth, desc);
                        }
                    }

                    // Draw popup if track was clicked on
                    if (tracks != null && tracks.size()>0 && (clickTrackX != -1) && showTracks && clickedTrack != -1 && clickedTrack < tracks.size()) {
                        //Log.d("PDFActivity", "onDraw: draw track and popup balloon. newWP="+newWP+" clickedWP="+clickedWP);
                        String desc = tracks.get(clickedTrack).getDesc();
                        drawPopup(canvas, clickTrackX, clickTrackY, boxWidth, desc);
                    }

                    //-----------------------
                    // Draw Current Location
                    //-----------------------
                    // Transparent Arc showing bearing (top of user screen)
                    //  CONVERT LAT LONG TO SCREEN COORDINATES
                    if (longNow != 0.0) {
                        currentLocationX = ((longNow - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                        currentLocationY = (((lat2 - latNow) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;

                        canvas.translate((float) currentLocationX, (float) currentLocationY);
                        // drawArc (RecF(left,top,right,bottom), starting arc in degrees (drawn clockwise, 0=3 o'clock,90=6 o'clock, 180=9 o'clock), finish arc in degrees, use center? paint)
                        // Draw the current location as a point on the map. Color of the point is defined in paint & outline above.
                        if (accuracy > 25) {
                            accuracy = accuracy / 2;
                            RectF rec = new RectF(-1 * accuracy, -1 * accuracy, accuracy, accuracy);
                            canvas.drawArc(rec, 0, 360, true, cyanTrans); // transparent blue circle
                        } else {
                            RectF rec = new RectF(-45f, -45f, 45f, 45f);
                            int arcSize = 90;
                            float startArc = bearing - 45;
                            if (startArc < 0) startArc = 360 + startArc;
                            canvas.drawArc(rec, startArc, arcSize, true, cyanTrans); // transparent blue arc
                            //TextView bTxt = (TextView)findViewById(R.id.debug);
                            //bTxt.setText("bearing="+(int)bearing+"  start="+(int)startArc);
                        }

                        //Log.d("latlong", "long="+longDiff+"    "+(int)Math.round(currentLocationY)+" long="+(int)Math.round(currentLocationX));

                        // DRAW POINT AT CURRENT LOCATION drawCircle(x,y,radius,paint)
                        // drawCircle(centerX,centerY,radius,paint)
                        canvas.drawCircle(0, 0, 20f, cyan); // final blue outline
                        canvas.drawCircle(0, 0, 19f, white); // larger white outline
                        canvas.drawCircle(0, 0, 15f, cyan); // blue center
                        // go back to 0,0
                        canvas.translate((float) -currentLocationX, (float) -currentLocationY);


                    }

                    // hide wait icon
                    wait.setVisibility(View.GONE);
                    // debug: offset
                    //canvas.translate(0,0);
                    //canvas.drawCircle(pdfView.getCurrentXOffset()-(float)currentLocationX,pdfView.getCurrentYOffset()-(float)currentLocationY,20f, red);

                    //Toast.makeText(PDFActivity.this,"Re-Draw "+count, Toast.LENGTH_SHORT).show();
            }).onLoad(nbPages -> {
                // SET LEVELS TO ZOOM TO WHEN DOUBLE CLICK, 34x44=3168, 22x34=2448
                if (mediaBoxWidth > 1500) {
                    pdfView.setMaxZoom(25f);// used to be 3.0f, 7, 20
                    pdfView.setMidZoom(7f);// used to be 1.75f 3.5
                } else {
                    pdfView.setMaxZoom(15f);// used to be 3.0f, 7, 20
                    pdfView.setMidZoom(3.5f);// used to be 1.75f 3.5
                }
                pdfView.setMinZoom(1f); // default is 1 (full document, no zoom)
                //Toast.makeText(PDFActivity.this,String.valueOf(nbPages), Toast.LENGTH_LONG).show();
                //Toast.makeText(PDFActivity.this,"Loaded file ", Toast.LENGTH_SHORT).show();

                // METADATA
                        /*PdfDocument.Meta meta = pdfView.getDocumentMeta();
                        Log.d(TAG, "title = " + meta.getTitle().toString());
                        Log.d(TAG, "author = " + meta.getAuthor());
                        Log.d(TAG, "subject = " + meta.getSubject());
                        Log.d(TAG, "keywords = " + meta.getKeywords());
                        Log.d(TAG, "creator = " + meta.getCreator());
                        Log.d(TAG, "producer = " + meta.getProducer());
                        Log.d(TAG, "creationDate = " + meta.getCreationDate());
                        Log.d(TAG, "modDate = " + meta.getModDate());*/

                // hide wait icon
                wait.setVisibility(View.GONE);
            }).load();
        } else {
            Toast.makeText(PDFActivity.this, "Cannot read file: " + path, Toast.LENGTH_LONG).show();
        }
    }
    /*public static float px2dp(Resources resource, float px) {
        // Convert pixels to dp (device independent)
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px, resource.getDisplayMetrics());
    }*/

    // find distance from a point to a line, used to determine if click on a track
    private float sqr(float x) { return x * x; }
    private float dist_sq(float x1, float y1, float x2, float y2) {
        // a^2 + b^2 = c^2 distance of a right angle triangle
        return sqr(x1 - x2) + sqr(y1 - y2); }
    private float distToSegmentSquared(float ptX, float ptY, float x1, float y1, float x2, float y2) {
        // pass point x,y then line segment x1,y1 x2,y2
        float line_dist = dist_sq(x1, y1, x2, y2);
        if (line_dist == 0) return dist_sq(ptX, ptY, x1, y1); // x1,y1 = x2,y2
        // Consider the line extending the segment, parameterized as v + t (w - v).  where v=x1,y1 w=x2,y2
        // We find projection of point p onto the line.
        // It falls where t = [(p-v) . (w-v)] / |w-v|^2
        // We clamp t from [0,1] to handle points outside the segment vw.
        float t = ((ptX - x1) * (x2 - x1) + (ptY - y1) * (y2 - y1)) / line_dist;
        t = Math.max(0, Math.min(1, t));
        float x = x1 + t * (x2 - x1);
        float y = y1 + t * (y2 - y1);
        return dist_sq(ptX, ptY, x, y);
    }


    private int findAUniqueName(){
        // find a unique name for waypoint labels
        int num = 1;
        boolean unique = true;
        boolean done = false;
        do {
            for (int i1 = 0; i1 < wayPts.size(); i1++) {
                if (wayPts.get(i1).getDesc().equals("Waypoint " + num)) {
                    unique = false;
                    num++;
                    break;
                }
            }
            if (unique) {
                done = true;
            } else {
                unique = true; // reset for next try
            }
        } while (!done);
        return num;
    }

    private int findAUniqueTrackName(){
        // find a unique name for track labels
        int num = 1;
        boolean unique = true;
        boolean done = false;
        do {
            for (int i1 = 0; i1 < tracks.size(); i1++) {
                if (tracks.get(i1).getDesc().equals("Track " + num)) {
                    unique = false;
                    num++;
                    break;
                }
            }
            if (unique) {
                done = true;
            } else {
                unique = true; // reset for next try
            }
        } while (!done);
        return num;
    }
    private Boolean checkForWaypointButtonClick(float boxWidth, float x, float y, double wayPtX, double wayPtY, int wpIndex) {
        // check for click on Edit, Move, or Delete button on waypoint or track popup
        if (adjustWP != -1 || deleting) return false; // if overlapping buttons only handle one event
        if (pdfView.getCurrentXOffset() + wayPtX <= screenWidth && pdfView.getCurrentXOffset() + x > 0) {
            // Test for balloon popup going off right side of screen
            int offsetBox = getOffsetXBox((int) wayPtX, boxWidth);

            // Test for balloon popup going off top of screen
            int offsetYBox = -1 * triangle_ht; //0;
            if (clickedTrack != -1){
                offsetYBox = startY - triangle_ht;
            }

            if ((wayPtY + pdfView.getCurrentYOffset()) < (pdfView.getHeight() / 2.0)) {
                offsetYBox = getOffsetYBox();
            }
            // Edit button click
            if (x >= ((wayPtX - (boxWidth / 2) - marg) + offsetBox) && x < (((wayPtX - (boxWidth / 2) - marg) + offsetBox) + marg + btn_size) &&
                    y < (wayPtY - startY + marg + offsetYBox) && y >= (wayPtY - startY - boxHt - marg + offsetYBox)){
                try {
                    if (clickedTrack == -1)
                        openEditWayPointActivity(wpIndex);
                    else
                        openEditTrackActivity(clickedTrack);
                    // hide wait icon
                    wait.setVisibility(View.GONE);
                    return false;
                } catch (OutOfMemoryError memoryError) {
                    Toast.makeText(PDFActivity.this, "Out of memory", Toast.LENGTH_SHORT).show();
                    return false;
                } catch (Exception error) {
                    Toast.makeText(PDFActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            // Move button click on a waypoint or Edit button click on a track
            else if (x >= (((wayPtX - (boxWidth / 2) - marg) + offsetBox) + marg + btn_size) && x < (((wayPtX - (boxWidth / 2) - marg) + offsetBox) + 2*marg + 2*btn_size) &&
                    y < (wayPtY - startY + marg + offsetYBox) && y >= (wayPtY - startY - boxHt - marg + offsetYBox)) {
                adjustWP = wpIndex;
                if (clickedTrack == -1)
                    moveWaypoint(wayPtX, wayPtY, wpIndex, moveIconWidth);
                else {
                    adjustWP = -1;
                    deleteTrack();
                }
            }
            // Delete button click
            else if (x >= ((wayPtX - (boxWidth / 2) - marg) + offsetBox) +2*marg + 2*btn_size && x < (((wayPtX - (boxWidth / 2) - marg) + offsetBox) + 3*marg + 3*btn_size) &&
                    y < (wayPtY - startY + marg + offsetYBox) && y >= (wayPtY - startY - boxHt - marg + offsetYBox)){
                clickedWP = wpIndex;
                deleting = true;
                deleteWaypoint();
            }
        }
        return true;
    }
    private void openEditWayPointActivity(int id) {
        // waypoint popup edit button was clicked
        // Open EditWayPointActivity
        Intent i1 = new Intent(PDFActivity.this, EditWayPointActivity.class);
        i1.putExtra("CLICKED", id);
        i1.putExtra("NAME", mapName);
        i1.putExtra("PATH", path);
        i1.putExtra("BOUNDS", strBounds);
        i1.putExtra("MEDIABOX", strMediaBox);
        i1.putExtra("VIEWPORT", strViewPort);
        startActivity(i1);
    }

    private void openEditTrackActivity(int id) {
        // waypoint popup edit button was clicked
        // Open EditWayPointActivity
        Intent i1 = new Intent(PDFActivity.this, EditTrackActivity.class);
        i1.putExtra("CLICKED", id); // tracks[id]
        i1.putExtra("NAME", mapName);
        i1.putExtra("PATH", path);
        i1.putExtra("BOUNDS", strBounds);
        i1.putExtra("MEDIABOX", strMediaBox);
        i1.putExtra("VIEWPORT", strViewPort);
        startActivity(i1);
    }

    private void moveWaypoint(double wayPtX,double wayPtY,int id, float moveIconWidth) {
        // waypoint popup move button was clicked
        moveIcon.setVisibility(View.VISIBLE);
        adjustX = (float) wayPtX + pdfView.getCurrentXOffset();
        adjustY = (float) wayPtY + pdfView.getCurrentYOffset();
        moveIcon.setX(adjustX - moveIconWidth / 2);
        moveIcon.setY(adjustY - moveIconWidth / 2);
        clickedWP = id;
        //adjustWP = clickedWP;
        // show menu
        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = PDFActivity.this.startActionMode(mActionModeCallback);
        TextView note = findViewById(R.id.move_instr);
        note.setVisibility(View.VISIBLE);
    }
    private void deleteWaypoint(){
        // waypoint popup delete button was clicked
        AlertDialog.Builder builder = new AlertDialog.Builder(PDFActivity.this);
        builder.setTitle("Delete:");
        String str = "Delete waypoint labeled: "+wayPts.get(clickedWP).getDesc()+"?";
        builder.setMessage(str).setPositiveButton("DELETE", dialogClickListener)
                .setNegativeButton("CANCEL", dialogClickListener).show();
        del_id = clickedWP;
    }

    private void deleteTrack(){
        // waypoint popup delete button was clicked
        AlertDialog.Builder builder = new AlertDialog.Builder(PDFActivity.this);
        builder.setTitle("Delete:");
        String str = "Delete track labeled: "+tracks.get(clickedTrack).getDesc()+"?";
        builder.setMessage(str).setPositiveButton("DELETE", trackDialogClickListener)
                .setNegativeButton("CANCEL", trackDialogClickListener).show();
        del_id = clickedTrack;
    }
    public int getOffsetXBox(float x,float popupWidth){
        // Return the adjustment for popup going off-screen horizontally
        // Test for balloon popup going off right side of screen
        int offsetBox = Math.round(screenWidth - (pdfView.getCurrentXOffset() + x + (popupWidth / 2)));

        // Test for balloon popup going off the left side of screen
        if (offsetBox >= 0) {
            offsetBox = 0;
            if ((pdfView.getCurrentXOffset() + x - (popupWidth / 2)) < 0)
                offsetBox = (-1 * Math.round(pdfView.getCurrentXOffset() + x - (popupWidth / 2)));
        }
        return offsetBox;
    }
    public int getOffsetYBox(){
        // startY = top of waypoint pin
        return startY + boxHt + triangle_ht;
    }
    public int getOffsetYTriangle(){
        return startY + 6 + triangle_ht;
    }

    public void drawPopup(Canvas canvas, float x, float y, float boxWidth, String desc) {
        // check if waypoint has scrolled off-screen
        if (pdfView.getCurrentXOffset() + x <= screenWidth && pdfView.getCurrentXOffset() + x > 0) {
            // truncate the description if it is longer than the popup
            float w = txtCol.measureText(desc);
            if (w > boxWidth - 2 * marg) {
                int chIndex = (int) (desc.length() * (boxWidth - 2 * marg) / w);
                desc = desc.substring(0, chIndex);
            }

            // Test for balloon popup going off right or left side of screen
            int offsetBox = getOffsetXBox(x, boxWidth);
            // Test for waypoint at bottom half of screen, display popup above
            int offsetYBox = -1 * triangle_ht;
            int offsetYTriangle = -1 * triangle_ht; // edit in drawTriangle too
            if (clickedTrack != -1){
                offsetYBox = startY-triangle_ht;
                offsetYTriangle = startY-triangle_ht;
            }
            //Log.d("Draw Popup","y="+y+"+ pdfViewYoffset="+pdfView.getCurrentYOffset()+" "+(y + pdfView.getCurrentYOffset())+" < "+(pdfView.getHeight() / 2.0));
            // Test for waypoint at top half of screen, display popup below
            if ((y + pdfView.getCurrentYOffset()) < (pdfView.getHeight() / 2.0)) {
                offsetYBox = getOffsetYBox();
                offsetYTriangle = getOffsetYTriangle();
            }
            // black border
            Paint recCol = new Paint();
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're using the light theme
                    // black border
                    recCol.setColor(Color.BLACK);
                    recCol.setStrokeWidth(3);
                    canvas.drawRect((x - (boxWidth / 2) - 3) + offsetBox, y + offsetYBox - startY - boxHt, (x + (boxWidth / 2) + 3) + offsetBox, y + offsetYBox - startY + 3, recCol);
                    // white rectangle
                    recCol.setColor(Color.WHITE);
                    recCol.setStrokeWidth(0); // solid fill
                    canvas.drawRect((x - (boxWidth / 2)) + offsetBox, y + offsetYBox - startY - boxHt + 3, (x + (boxWidth / 2)) + offsetBox, y + offsetYBox - startY, recCol);
                    drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                    // white triangle, reset text to black
                    txtCol.setColor(Color.BLACK);
                    canvas.drawText(desc, (x - (boxWidth / 2)) + marg + offsetBox, y + offsetYBox - startY - (boxHt * 0.75f) - 5 + (txtSize / 2.0f), txtCol);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // Night mode is active, we're using dark theme
                    // White border
                    recCol.setColor(Color.BLACK);
                    recCol.setStrokeWidth(3);
                    canvas.drawRect((x - (boxWidth / 2) - 3) + offsetBox, y + offsetYBox - startY - boxHt, (x + (boxWidth / 2) + 3) + offsetBox, y + offsetYBox - startY + 3, recCol);
                    // black rectangle
                    recCol.setColor(Color.GRAY);
                    recCol.setStrokeWidth(0); // solid fill
                    canvas.drawRect((x - (boxWidth / 2)) + offsetBox, y + offsetYBox - startY - boxHt + 3, (x + (boxWidth / 2)) + offsetBox, y + offsetYBox - startY, recCol);
                    drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                    // white text
                    txtCol.setColor(Color.WHITE);
                    canvas.drawText(desc, (x - (boxWidth / 2)) + marg + offsetBox, y + offsetYBox - startY - (boxHt * 0.75f) - 5 + (txtSize / 2.0f), txtCol);
                    break;
            }

            canvas.save();
            // add edit/move/delete buttons below label
            canvas.translate(x - boxWidth / 2.0f + offsetBox, y + offsetYBox - startY - boxHt / 2.0f);
            // Edit button
            Drawable editImg = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_edit, getTheme());//getResources().getDrawable(R.drawable.ic_edit, null);
            assert editImg != null;
            editImg.setBounds(0, 0, btn_size, btn_size);
            editImg.draw(canvas);
            canvas.translate(btn_size + marg, 0);
            // don't show moveImg when click on track
            if (clickedTrack == -1) {
                // move button
                Drawable moveImg = ResourcesCompat.getDrawable(getResources(), R.drawable.move_pin, getTheme());//getResources().getDrawable(R.drawable.ic_my_location, null);
                assert moveImg != null;
                moveImg.setBounds(0, 0, btn_size, btn_size);
                moveImg.draw(canvas);
                canvas.translate(btn_size + marg, 0);
            }
            // delete button
            Drawable deleteImg = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete, getTheme());//getResources().getDrawable(R.drawable.ic_delete, null);
            assert deleteImg != null;
            deleteImg.setBounds(0, 0, btn_size, btn_size);
            deleteImg.draw(canvas);
            canvas.translate((x + (boxWidth / 2)) + offsetBox + 10, y + offsetYBox - startY - boxHt - 12);
            canvas.restore();
        }
    }
    public void drawLabels(Canvas canvas, float x, float y, float boxWidth, String desc){
        // check if waypoint has scrolled off-screen
        if (pdfView.getCurrentXOffset() + x <= screenWidth && pdfView.getCurrentXOffset() + x > 0) {
            // truncate the description if it is longer than the popup
            float w = txtCol.measureText(desc);
            int labelHt = txtSize + (marg * 2);
            if (w > boxWidth - 2*marg) {
                int chIndex = (int) (desc.length() * (boxWidth - 2 * marg) / w);
                desc = desc.substring(0, chIndex);
            }

            // Test for label going off right or left side of screen
            int offsetBox = getOffsetXBox(x, boxWidth);
            // Test for waypoint at top half of screen, display popup below
            int offsetYBox = -1 * triangle_ht;
            int offsetYTriangle = -1 * triangle_ht; // edit in drawTriangle too
            //Log.d("Draw Popup","y="+y+"+ pdfViewYoffset="+pdfView.getCurrentYOffset()+" "+(y + pdfView.getCurrentYOffset())+" < "+(pdfView.getHeight() / 2.0));
            if ((y + pdfView.getCurrentYOffset()) < (pdfView.getHeight() / 2.0)) {
                offsetYBox = startY + labelHt + triangle_ht;
                offsetYTriangle = getOffsetYTriangle();
            }
            // black border
            Paint recCol = new Paint();
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're using the light theme
                    // black border
                    recCol.setColor(Color.BLACK);
                    recCol.setStrokeWidth(3);
                    canvas.drawRect((x - (boxWidth / 2) - 3) + offsetBox, y + offsetYBox - startY - labelHt, (x + (boxWidth / 2) + 3) + offsetBox, y + offsetYBox - startY + 3, recCol);
                    // white rectangle
                    recCol.setColor(Color.WHITE);
                    recCol.setStrokeWidth(0); // solid fill
                    canvas.drawRect((x - (boxWidth / 2)) + offsetBox, y + offsetYBox - startY - labelHt + 3, (x + (boxWidth / 2)) + offsetBox, y + offsetYBox - startY, recCol);
                    drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                    // white triangle, reset text to black
                    txtCol.setColor(Color.BLACK);
                    canvas.drawText(desc, (x - (boxWidth / 2)) + marg + offsetBox, y + offsetYBox - startY - (labelHt * 0.75f) + 12 + (txtSize / 2.0f), txtCol);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // Night mode is active, we're using dark theme
                    // White border
                    recCol.setColor(Color.BLACK);
                    recCol.setStrokeWidth(3);
                    canvas.drawRect((x - (boxWidth / 2) - 3) + offsetBox, y + offsetYBox - startY - labelHt, (x + (boxWidth / 2) + 3) + offsetBox, y + offsetYBox - startY + 3, recCol);
                    // black rectangle
                    recCol.setColor(Color.GRAY);
                    recCol.setStrokeWidth(0); // solid fill
                    canvas.drawRect((x - (boxWidth / 2)) + offsetBox, y + offsetYBox - startY - labelHt + 3, (x + (boxWidth / 2)) + offsetBox, y + offsetYBox - startY, recCol);
                    drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                    // white text
                    txtCol.setColor(Color.WHITE);
                    canvas.drawText(desc, (x - (boxWidth / 2)) + marg + offsetBox, y + offsetYBox - startY - (labelHt * 0.75f) + 12 + (txtSize / 2.0f), txtCol);
                    break;
            }

            canvas.save();
            // add edit/move/delete buttons below label
            /*canvas.translate(x - boxWidth/2.0f + offsetBox,y + offsetYBox - startY - boxHt/2.0f);
            Drawable editImg = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_edit, getTheme());//getResources().getDrawable(R.drawable.ic_edit, null);
            assert editImg != null;
            editImg.setBounds(0, 0, btn_size, btn_size);
            editImg.draw(canvas);
            canvas.translate(btn_size + marg,0);
            Drawable moveImg = ResourcesCompat.getDrawable(getResources(), R.drawable.move_pin, getTheme());//getResources().getDrawable(R.drawable.ic_my_location, null);
            assert moveImg != null;
            moveImg.setBounds(0, 0, btn_size, btn_size);
            moveImg.draw(canvas);
            canvas.translate(btn_size + marg,0);
            Drawable deleteImg = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete, getTheme());//getResources().getDrawable(R.drawable.ic_delete, null);
            assert deleteImg != null;
            deleteImg.setBounds(0, 0, btn_size, btn_size);
            deleteImg.draw(canvas);
            canvas.translate((x + (boxWidth / 2)) + offsetBox + 10, y + offsetYBox - startY - boxHt - 12);
            canvas.restore();*/
        }
    }
    public void drawTriangle(Canvas canvas, Paint paint, int x, int y, int offsetYBox) {
        // ----  White triangle with black v. For waypoint balloon.
        // \  /
        //  \/
        // if offsetYBox > 0 draw up arrow
        int width = Math.round(getResources().getDimension(R.dimen.triangle_width));
        int halfWidth = width / 2;
        Path path = new Path();
        // down arrow
        if (offsetYBox == -1 * triangle_ht || offsetYBox == startY-triangle_ht) {
        //if(offsetYBox > 0 || offsetYBox == triangle_ht){
            path.moveTo(x - halfWidth, y); // Top left
            path.lineTo(x, y + triangle_ht); //  Bottom
            path.lineTo(x + halfWidth, y); // Top right
            path.lineTo(x - halfWidth, y); // Back to Top left
        }
        // up arrow
        else{
            path.moveTo(x - halfWidth, y); // Bottom left
            path.lineTo(x, y - triangle_ht); // Top
            path.lineTo(x + halfWidth, y); // Bottom right
            path.lineTo(x - halfWidth, y); // Back to Bottom left
        }
        path.close();

        canvas.drawPath(path, paint);
        Paint outline = new Paint();
        outline.setColor(Color.BLACK);
        outline.setStrokeWidth(3);
        // down arrow
        if (offsetYBox == -1 * triangle_ht || offsetYBox == startY-triangle_ht){
        //if (offsetYBox > 0  || offsetYBox == triangle_ht){
            canvas.drawLine(x - halfWidth, y + 3, x, y + triangle_ht, outline);
            canvas.drawLine(x + halfWidth, y + 3, x, y + triangle_ht, outline);
        }
        // up arrow
        else{
            canvas.drawLine(x - halfWidth, y - 3, x, y - triangle_ht, outline);
            canvas.drawLine(x + halfWidth, y - 3, x, y - triangle_ht, outline);
        }
        // erase black line at base of triangle
        outline.setColor(paint.getColor());
        canvas.drawLine(x-halfWidth+3,y,x+halfWidth-3,y,outline);
    }

    protected void updatePageSize(){
        optimalPageWidth.set((double)pdfView.getPageSize(0).getWidth()); // pdfView.getOptimalPageWidth();
        optimalPageHeight.set((double)pdfView.getPageSize(0).getHeight()); // pdfView.getOptimalPageHeight();
        //Log.d("page size","width="+optimalPageWidth.get()+" height="+optimalPageHeight.get());
    }

    public float calculateDistance(double startLat, double startLng, double endLat, double endLng) {
        float[] results = new float[1];
        // The result is written directly into the first index of the array
        Location.distanceBetween(startLat, startLng, endLat, endLng, results);
        return results[0]; // Returns distance in meters
    }

    @Override
    protected void onResume() {
        super.onResume();

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
        //startLocationUpdates();

        // read user preferences from DBHandler SETTINGS_TABLE
        // read waypoints and tracks for this map
        try {
            db = new DBWayPtHandler(PDFActivity.this);
            dbTrack = new DBTrackHandler(PDFActivity.this);
            dbExecutor.execute(() -> {
                DBHandler db2 = DBHandler.getInstance(PDFActivity.this);
                loadAdjacentMaps = db2.getLoadAdjMaps() != 0;
                if (stateShowAllWayPts == -1)
                    showAllWayPts = db2.getShowWaypoints() != 0;
                if (stateShowTracks == -1)
                    showTracks = db2.getShowTracks() != 0;
                showAllWayPtLabels = db2.getShowAllWaypointLabels() != 0;
                myMap = db2.getMap(mapName);
                // set orientation for this map
                portraitLocked = myMap.getMapOrientation().equals("portrait");
                landscapeLocked = myMap.getMapOrientation().equals("landscape");
                // get all maps for load adjacent maps and lock map in portrait or landscape
                try {
                    maps = db2.getAllMaps();
                }catch (SQLException | NullPointerException e) {
                    Toast.makeText(PDFActivity.this, getResources().getString(R.string.problemReadingDatabase) + e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
            /*db2 = new DBHandler(PDFActivity.this);
            loadAdjacentMaps = db2.getLoadAdjMaps() != 0;
            if (stateShowAllWayPts == -1)
                showAllWayPts = db2.getShowWaypoints() != 0;
            if (stateShowTracks == -1)
                showTracks = db2.getShowTracks() != 0;
            showAllWayPtLabels = db2.getShowAllWaypointLabels() != 0;
            // set orientation for this map
            myMap = db2.getMap(mapName);
            portraitLocked = myMap.getMapOrientation().equals("portrait");
            landscapeLocked = myMap.getMapOrientation().equals("landscape");
            // get all maps for load adjacent maps and lock map in portrait or landscape
            try {
                maps = db2.getAllMaps();
            }catch (SQLException | NullPointerException e) {
                Toast.makeText(PDFActivity.this, getResources().getString(R.string.problemReadingDatabase) + e.getMessage(),Toast.LENGTH_LONG).show();
            }*/
            // Update Waypoints
            wayPts = db.getWayPts(mapName);
            // Update Tracks
            tracks = dbTrack.getTracks(mapName);
        }catch (Exception e){
            Toast.makeText(PDFActivity.this,getResources().getString(R.string.problemReadingDatabase)+e.getMessage(),Toast.LENGTH_LONG).show();
        }

        wayPts.SortPts();
        clickedWP = -1; // hide balloon
        lastClickedWP = -1;
        clickedTrack = -1;
        clickTrackX = -1.0f;
        clickTrackY = -1.0f;
        adjustWP = -1;
        newWP = false;
        adjacentMapsBtnShowing = false;
        // Start Screen Sensor Listener
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);

        // if user had checked lock orientation, then apply it when return from help or edit waypoint 6/22/22
        if (landscapeLocked){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
        else if (portraitLocked){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    /*@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("showAllWayPts", showAllWayPts);
        savedInstanceState.putBoolean("showTracks", showTracks);
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        boolean showAllWayPts = savedInstanceState.getBoolean("showAllWayPts");
        boolean showTracks = savedInstanceState.getBoolean("showTracks");
    }*/

    @Override
    protected void onPause() {
        super.onPause();
        // save state of show all waypoints (set for the session if off but added a waypoint
        if (showAllWayPts)
            stateShowAllWayPts = 1;
        else
            stateShowAllWayPts = 0;
        // save state of tracking
        if (showTracks)
            stateShowTracks = 1;
        else
            stateShowTracks = 0;
        // Location Service
        // Unregister to prevent memory leaks when app is in background
        unregisterReceiver(locationReceiver);
        // TODO remove
        // Location Service
        // Unregister to prevent memory leaks when app is in background
        //stopLocationUpdates();
        //Log.d("PDFActivity:onPause","close dbWayPtHandler, stop location updates");
        db.close();
        //db2.close();
        db = null;
        //db2 = null;
        // Stop Screen Sensor Listener
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 2. Bind to the service when the activity becomes visible
        Intent intent = new Intent(this, TrackingService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind Location Service when the activity is no longer visible to prevent memory leaks
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    //  LOCATION UPDATES
    private void updateUIWithServiceData() {
        // update tracking distance for current track
        if (isBound && trackingService != null) {
            float distance = trackingService.getTotalDistance();
            distanceTextView.setText("Distance from Service: " + distance + "m");
        }
    }

    // Add this inside SecondActivity.java just like you did in MainActivity
    private class LocationUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "ACTION_LOCATION_UPDATE".equals(intent.getAction())) {
                // save last location so we can see how much they moved
                latBefore = latNow;
                longBefore = longNow;

                latNow = intent.getDoubleExtra("extra_latitude", 0.0);
                longNow = intent.getDoubleExtra("extra_longitude", 0.0);
                accuracy = intent.getFloatExtra("extra_accuracy", 0.0f);

                // Add to the current track's path with current location if on map
                Track currentTrack = new Track();
                if (latBefore != -1 && currentTrackID != -1 &&
                        (latNow >= lat1 && latNow <= lat2) &&
                        (longNow >= long1 && longNow <= long2)) {
                    currentTrack.addTrackSegment((float) longBefore, (float) latBefore, (float) longNow, (float) latNow);
                    // Save new line segment in database
                    dbTrack.updateTrack(currentTrack);
                }

                // **Debug** make it simulate user movement to draw a track
                if (latBefore != -1){
                    Random rand = new Random();
                    int randomInt = 1;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        randomInt = rand.nextInt(1,9);
                    }
                    if (randomInt > 7) randomInt = randomInt * -1;
                    double r = (double)randomInt / 10000.0;
                    latNow =  latBefore + r;
                    randomInt = 1;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        randomInt = rand.nextInt(1,9);
                    }
                    if (randomInt > 7) randomInt = randomInt * -1;
                    r = (double)randomInt / 10000.0;
                    longNow = longBefore + r;
                }

                //bearing = location.getBearing(); // 0-360 degrees 0 at North

                // Makes top of map (north) off
                    /*geoField = new GeomagneticField(
                            Double.valueOf(latNow).floatValue(),
                            Double.valueOf(longNow).floatValue(),
                            Double.valueOf(location.getAltitude()).floatValue(),
                            System.currentTimeMillis()
                    );*/
                //bearing += geoField.getDeclination(); // Adjust for declination - difference between magnetic north and true north. Phone returns magnetic north.
                //bearing -= 90; // Adjust by 90 degrees. Canvas needs 0 at East, this returns 0 at North
                //if (bearing<0) bearing = 360 + bearing;

                // debug
                //TextView bTxt = (TextView)findViewById(R.id.debug);
                //bTxt.setText(Float.toString(bearing)+"  adjust: "+Float.toString((geoField.getDeclination()))+ "  bear: "+Float.toString(location.getBearing()));

                pdfView.invalidate();
                //
                // Load Adjacent Maps?
                // check if we need to display load adjacent maps button because current location is within 1/4 mile of other maps
                //

                // OLD WAY when current location goes off edge. Problem USGS and FS maps have a margin CPW maps do not. Show when close to edge.
                //double percentX = 0.13;
                //double percentY = 0.10;
                //******************************
                // DEBUG force current location
                //******************************
                //latNow = lat2 - latDiff*percentY;
                //longNow = long2 - longDiff*percentX;
                //latNow = lat1 + latDiff*percentY;
                //longNow = long1 + longDiff*percentX;
                //if (loadAdjacentMaps && onMap && (latNow < (lat1 + latDiff*percentX)  || latNow > (lat2 - latDiff*percentX)  || longNow < (long1 + longDiff*percentY) || longNow > (long2 - longDiff*percentY))){

                double quarterMileInDegrees = 0.00458; // 1 degree = 54.6 miles
                if (loadAdjacentMaps &&
                        latNow > (lat1 - quarterMileInDegrees) &&
                        latNow < (lat2 + quarterMileInDegrees) &&
                        longNow > (long1 - quarterMileInDegrees) &&
                        longNow < (long2 + quarterMileInDegrees)){
                    // Get list of all available maps and see if the current location is on or within a 1/4 mile of one or more of them
                    ArrayList<Integer> mapIds = new ArrayList<>();// PDF maps that the current location is on
                    if (maps == null) return;
                    for (int i = 0; i < maps.size(); i++) {
                        PDFMap map = maps.get(i);
                        if (map.getName().equals(mapName)) continue; // don't list current map
                        String bounds = map.getBounds(); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
                        if (bounds == null || bounds.isEmpty())
                            return; // it will be 0 length if it is importing
                        bounds = bounds.trim(); // remove leading and trailing spaces

                        // Get Latitude, Longitude bounds.
                        // aLat1 and aLong1 are the smallest values SW corner
                        // aLat2 and aLong2 are the largest NE corner
                        String[] arrLatLong = bounds.split(" ");
                        // convert strings to double
                        Double[] LatLong = new Double[arrLatLong.length];
                        for (int l = 0; l < arrLatLong.length; l++) {
                            LatLong[l] = Double.parseDouble(arrLatLong[l]);
                        }
                        // Find the smallest and largest values
                        double aLat1 = LatLong[0];
                        double aLong1 = LatLong[1];
                        double aLat2 = LatLong[0];
                        double aLong2 = LatLong[1];
                        for (int l = 0; l < LatLong.length; l = l + 2) {
                            if (LatLong[l] < aLat1) aLat1 = LatLong[l];
                            if (LatLong[l] > aLat2) aLat2 = LatLong[l];
                            if (LatLong[l + 1] < aLong1) aLong1 = LatLong[l + 1];
                            if (LatLong[l + 1] > aLong2) aLong2 = LatLong[l + 1];
                        }

                        // Is current location on this map? Add it to mapIds (array of maps that contain the current location)
                        // On map
                        if (latNow >= aLat1 && latNow <= aLat2 && longNow >= aLong1 && longNow <= aLong2) {
                            mapIds.add(i);
                        }
                    }
                        /*if (mapIds.size()==0){
                            //Toast.makeText(PDFActivity.this,"No adjacent maps found to load.",Toast.LENGTH_SHORT).show();
                        }
                        /*else if (mapIds.size()==1){
                            // Only one map found that contains the current location. Load it.
                            loadNewMap(maps, mapIds.get(0));
                            Toast.makeText(PDFActivity.this,"Now showing adjacent map.",Toast.LENGTH_SHORT).show();
                        }*/
                    if (!mapIds.isEmpty()) {
                        // Several maps found. Display button and menu to load new map.
                        //Toast.makeText(PDFActivity.this,"Several adjacent maps are available",Toast.LENGTH_SHORT).show();
                        menuBtn.setVisibility(View.VISIBLE);
                        adjacentMapsBtnShowing = true; // if they don't click on the button but click elsewhere, use this to hide the menuBtn in pdfView tap event.
                        menuBtn.setOnClickListener(view -> {
                            PopupMenu popup = new PopupMenu(PDFActivity.this, menuBtn);
                            popup.getMenuInflater().inflate(R.menu.adjacent_maps_menu, popup.getMenu());
                            for (int j = 0; j < mapIds.size(); j++) {
                                // add(groupId, itemId, order, title) Pass the index into maps array as the itemId
                                popup.getMenu().add(1, mapIds.get(j), j + 1, maps.get(mapIds.get(j)).getName());
                            }

                            popup.show();
                            popup.setOnMenuItemClickListener(item -> {
                                // load the user selected map
                                int i1 = item.getItemId();
                                loadNewMap(maps, i1);
                                return true;
                            });
                            // hide the Load Adjacent Maps button
                            popup.setOnDismissListener(menu -> {
                                Button menuBtn = findViewById(R.id.load_adjacent_maps);
                                menuBtn.setVisibility(View.GONE);
                                adjacentMapsBtnShowing = false;
                            });
                        });
                    }
                }
                else{
                    menuBtn.setVisibility(View.GONE);
                    adjacentMapsBtnShowing = false;
                }
            }
        }
    }

    // TODO remove
    /*private void startLocationUpdates() {
        LocationRequest mLocationRequest;
        if (Build.VERSION.SDK_INT >= 31){
            mLocationRequest = new LocationRequest.Builder(1000)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setIntervalMillis(1000)
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

        if ((ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
            }
        }
    }*/

    // TODO remove
    /*private void stopLocationUpdates() {
        if ((ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
        }
    }*/


    // -------------------------------------------------
    //    Screen Orientation or Screen Rotate Event
    // -------------------------------------------------
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Reset screen width and height
         if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics deviceWindowMetrics = getApplicationContext().getSystemService(WindowManager.class).getMaximumWindowMetrics();
            screenWidth = deviceWindowMetrics.getBounds().width();
        } else if (Build.VERSION.SDK_INT >= 28) {
            screenWidth = getResources().getDisplayMetrics().widthPixels;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
        }
        // Checks the orientation of the screen for landscape and portrait
         if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)  {
            landscape = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            landscape = false;
        }
    }

    /*public void zoomToCurrentLocation(float x,float y){
        // not working???
        PointF pivot = new  PointF(x,y);
        pdfView.zoomCenteredTo(3,pivot);
    }*/


    // -------------------------------------------------
    //     Sense Screen Spin (X Axis) Event.
    //     Point phone in new direction.
    // -------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer){
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;

            // mOrentation[0] is azimuth, mOrientation[1] is pitch, and mOrientation[2] is roll
            // if the roll is positive you're in reverse landscape (landscape right)
            // and if the roll is negative you're in landscape (landscape left).
            // -12 to correct for magnetic north versus true north
            if (landscape) {
                // landscape left
                if (mOrientation[2] < 0) {
                    adjust = 90;
                }
                // landscape right
                else {
                    adjust = 270;
                }
                bearing = azimuthInDegress +12 + adjust - 90; // Adjust for landscape. Subtract 90 degrees because canvas needs 0 at the East, this returns 0 at the North.
            }
            else
                bearing = azimuthInDegress +12 - 90;
            if (bearing < 0) bearing = 360 + bearing;
            if (bearing > 360) bearing = bearing - 360;
            //TextView bTxt = (TextView)findViewById(R.id.debug);
            //bTxt.setText("bearing="+(int)bearing+"  Z="+(int)mOrientation[2] );
            // Debug
            //TextView bTxt = (TextView)findViewById(R.id.debug);
            //if (landscape && mOrientation[2] < 0)
            //    bTxt.setText("azimuthInDegrees="+(int)azimuthInDegress+"  landscape-left   Bearing: "+(int)bearing);
            //else if (landscape)
            //    bTxt.setText("azimuthInDegrees="+(int)azimuthInDegress+"  landscape-right   Bearing: "+(int)bearing);
            //else
            //    bTxt.setText("azimuthInDegrees="+(int)azimuthInDegress+"  portrait   Bearing: "+(int)bearing);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required method
    }

    // -------------
    //   ... Menu
    // -------------
    MenuItem action_portrait;
    MenuItem action_landscape;
    MenuItem action_showAll;
    MenuItem action_showWayPts;
    MenuItem action_showTracks;
    MenuItem action_loadAdjacentMaps;
    MenuItem settingsItem;
    /*DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @SuppressLint("SourceLockedOrientationActivity")
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    // Lock LANDSCAPE
                    action_landscape.setChecked(true);
                    action_portrait.setChecked(false);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    landscape = true;
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // Lock PORTRAIT
                    action_landscape.setChecked(false);
                    action_portrait.setChecked(true);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    landscape = false;
                    break;
            }
        }
    };*/

    // for Tracking Configuration icon to show up
    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mapMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_menu, menu);
        //Lock in the current orientation
        action_portrait = menu.findItem(R.id.action_portrait);
        action_landscape = menu.findItem(R.id.action_landscape);
        action_showAll = menu.findItem(R.id.action_showAll);
        action_showWayPts = menu.findItem(R.id.action_showWayPts);
        action_showTracks = menu.findItem(R.id.action_showTracks);
        action_loadAdjacentMaps = menu.findItem(R.id.action_loadAdjacentMaps);
        wayPtMenuItem = menu.findItem(R.id.action_add_way_pt);
        trackMenuItem = menu.findItem(R.id.action_add_track);
        action_loadAdjacentMaps.setChecked(loadAdjacentMaps);
        action_portrait.setChecked(portraitLocked);
        action_landscape.setChecked(landscapeLocked);
        action_showAll.setChecked((showAllWayPtLabels));
        action_showWayPts.setChecked(showAllWayPts);
        action_showTracks.setChecked(showTracks);

        // Force Android to display icons inside the 3-dot overflow dropdown
        if (menu instanceof MenuBuilder) {
            MenuBuilder menuBuilder = (MenuBuilder) menu;
            menuBuilder.setOptionalIconsVisible(true);
        }
        /*settingsItem = menu.findItem(R.id.action_tracking_settings);
        // Set up Tracking Settings
        if (settingsItem != null) {
            // Unpack the layout view reference bound to the action layer
            View actionView = settingsItem.getActionView();

            if (actionView != null) {
                TextView intervalLabel = actionView.findViewById(R.id.menu_interval_label);
                SeekBar seekBar = actionView.findViewById(R.id.menu_interval_seekbar);
                Button btnAuto = actionView.findViewById(R.id.menu_btn_reset_auto);

                // Set up your SeekBar listener
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (progress < 1) progress = 1;
                        intervalLabel.setText("Manual Interval: " + progress + "s");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        int progress = seekBar.getProgress();
                        if (progress < 1) progress = 1;

                        // Send updated long variables directly to TrackingService via Intent
                        Intent intent = new Intent(PDFActivity.this, TrackingService.class);
                        intent.putExtra("update_interval", progress * 1000L);
                        intent.putExtra("update_fastest_interval", (progress * 1000L) / 2);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent);
                        } else {
                            startService(intent);
                        }
                    }
                });

                // Set up your Button listener
                btnAuto.setOnClickListener(v -> {
                    Intent intent = new Intent(PDFActivity.this, TrackingService.class);
                    intent.putExtra("enable_auto", true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                    intervalLabel.setText("Interval: Controlled by Speed");
                    // OPTIONAL FIX: Programmatically collapses the tracking layout back
                    // into a simple settings icon once auto-tracking is engaged.
                    settingsItem.collapseActionView();
                });
            }
        }*/

        return true;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_markCurrent){
            TextView pTxt = findViewById(R.id.cur_pos);
            String str = getString(R.string.CurPos) + String.format(Locale.US,"%.05f", latNow) + ", " + String.format(Locale.US,"%.05f", longNow);
            // check if current location is on this map
            if (pTxt.getText().equals(str)) {
                markCurrent = true;
                showAllWayPts = true;
                action_showWayPts.setChecked(true);
            }
            else {
                markCurrent = false;
                Toast.makeText(PDFActivity.this,"Current location not on map", Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.action_deleteAll){
            try {
                deleting = true; // don't register click events until done deleting
                clickedWP = -1;
                newWP = false;
                // display alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(PDFActivity.this);
                builder.setTitle("Delete");
                builder.setMessage("Delete all waypoints?").setPositiveButton("DELETE", dialogAllClickListener)
                        .setNegativeButton("CANCEL", dialogAllClickListener).show();
            }catch (SQLException e){
                Toast.makeText(PDFActivity.this,"Problem deleting "+mapName, Toast.LENGTH_LONG).show();
            }
        }
        // All Waypoint Labels
        else if (id == R.id.action_showAll){
            // isChecked() returns the state before the user clicked on it
            // uncheck show all labels
            if (action_showAll.isChecked()){
                action_showAll.setChecked(false);
                showAllWayPtLabels = false;
                dbExecutor.execute(() -> {
                    DBHandler.getInstance(PDFActivity.this).setShowAllWaypointLabels(0);
                });
                //db2.setShowAllWaypointLabels(0);
            }
            // check show all labels, also turn on show waypoints
            else{
                action_showAll.setChecked(true);
                showAllWayPtLabels = true;
                action_showWayPts.setChecked(true);
                showAllWayPts = true;
                dbExecutor.execute(() -> {
                    DBHandler db2 = DBHandler.getInstance(PDFActivity.this);
                    db2.setShowAllWaypointLabels(1);
                    db2.setShowWaypoints(1);
                });
            }
        }
        // Waypoints
        else if (id == R.id.action_showWayPts){
            // isChecked() returns the state before the user clicked on it
            // uncheck waypoints
            if (action_showWayPts.isChecked()){
                action_showWayPts.setChecked(false);
                showAllWayPts = false;
                dbExecutor.execute(() -> {
                    DBHandler.getInstance(PDFActivity.this).setShowWaypoints(0);
                });
                //db2.setShowWaypoints(0);
            }
            // check waypoints
            else{
                action_showWayPts.setChecked(true);
                showAllWayPts = true;
                dbExecutor.execute(() -> {
                    DBHandler.getInstance(PDFActivity.this).setShowWaypoints(1);
                });
                //db2.setShowWaypoints(1);
            }
        }
        // Show Tracking
        else if (id == R.id.action_showTracks){
            // isChecked() returns the state before the user clicked on it
            // uncheck tracks
            if (action_showTracks.isChecked()){
                action_showTracks.setChecked(false);
                showTracks = false;
                dbExecutor.execute(() -> {
                    DBHandler.getInstance(PDFActivity.this).setShowTracks(0);
                });
                // db2.setShowTracks(0);
            }
            // check tracks
            else{
                action_showTracks.setChecked(true);
                showTracks = true;
                dbExecutor.execute(() -> {
                   DBHandler.getInstance(PDFActivity.this).setShowTracks(1);
                });
                //db2.setShowTracks(1);
            }
        }
        // Show AdjacentMaps when current location is on or close to other maps
        else if (id == R.id.action_loadAdjacentMaps){
            if (action_loadAdjacentMaps.isChecked()){
                action_loadAdjacentMaps.setChecked(false);
                loadAdjacentMaps = false;
                dbExecutor.execute(() -> {
                    DBHandler.getInstance(PDFActivity.this).setLoadAdjMaps(0);
                });
                //db2.setLoadAdjMaps(0);
            }
            else{
                action_loadAdjacentMaps.setChecked(true);
                loadAdjacentMaps = true;
                dbExecutor.execute(() -> {
                    DBHandler.getInstance(PDFActivity.this).setLoadAdjMaps(1);
                });
                //db2.setLoadAdjMaps(1);
            }
        }
        else if (id == R.id.action_portrait){
            if (!action_portrait.isChecked()) {
                action_portrait.setChecked(true);
                action_landscape.setChecked(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                try{
                    myMap.setMapOrientation("portrait");// update user preference
                    dbExecutor.execute(() -> {
                        DBHandler.getInstance(PDFActivity.this).updateMap(myMap);
                    });
                    //db2.updateMap(myMap);
                } catch (Exception e){
                    Toast.makeText(PDFActivity.this,getResources().getString(R.string.problemReadingDatabase)+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                landscape = false;
                portraitLocked = true;
                landscapeLocked = false;
            }
            else {
                try{
                    myMap.setMapOrientation("none");// update user preference
                    dbExecutor.execute(() -> {
                        DBHandler.getInstance(PDFActivity.this).updateMap(myMap);
                    });
                    //db2.updateMap(myMap);
                } catch (Exception e){
                    Toast.makeText(PDFActivity.this,getResources().getString(R.string.problemReadingDatabase)+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                action_portrait.setChecked(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                portraitLocked = false;
                landscapeLocked = false;
            }
        }
        else if (id == R.id.action_landscape){
            if (!action_landscape.isChecked()) {
                action_portrait.setChecked(false);
                action_landscape.setChecked(true);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                try{
                    myMap.setMapOrientation("landscape");// update user preference
                    dbExecutor.execute(() -> {
                        DBHandler.getInstance(PDFActivity.this).updateMap(myMap);
                    });
                    //db2.updateMap(myMap);
                } catch (Exception e){
                    Toast.makeText(PDFActivity.this,getResources().getString(R.string.problemReadingDatabase)+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                landscape = true;
                portraitLocked = false;
                landscapeLocked = true;
            } else {
                try{
                    myMap.setMapOrientation("none");// update user preference
                    dbExecutor.execute(() -> {
                        DBHandler.getInstance(PDFActivity.this).updateMap(myMap);
                    });
                    //db2.updateMap(myMap);
                } catch (Exception e){
                    Toast.makeText(PDFActivity.this,getResources().getString(R.string.problemReadingDatabase)+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                action_landscape.setChecked(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                portraitLocked = false;
                landscapeLocked = false;
            }
        }
        /*else if (id == R.id.action_lock_orient){
            AlertDialog.Builder builder = new AlertDialog.Builder(PDFActivity.this);
            builder.setTitle("Lock Orientation:");
            builder.setMessage("Lock map orientation in which mode?").setPositiveButton("LANDSCAPE", dialogClickListener)
                        .setNegativeButton("PORTRAIT", dialogClickListener).show();
            return true;
        }*/
        // Turn Add WayPoint On/Off
        else if (id == R.id.action_add_way_pt) {
            // turn off add waypoint pin
            if (addWayPtFlag){
                addWayPtFlag = false;
                wayPtMenuItem.setIcon(R.mipmap.ic_grey_pin_forgnd);
            }
            // turn on add waypoint pin
            else {
                addWayPtFlag = true;
                clickedWP = -1; // hide balloon popups
                newWP = false;
                showAllWayPts = true;
                action_showWayPts.setChecked(true);
                wayPtMenuItem.setIcon(R.mipmap.ic_cyan_pin_forgnd);
                Toast.makeText(PDFActivity.this, getResources().getString(R.string.wayPtInstr), Toast.LENGTH_LONG).show();
            }
        }
        // Turn Tracking On/Off
        else if (id == R.id.action_add_track) {
            // turn off add track icon
            if (addTrackFlag){
                turnTrackingOff();
            }
            // turn on add track icon
            else {
                int num = findAUniqueTrackName();
                tracks.add(mapName,"Track "+num, "cyan", null);
                currentTrackID = tracks.size()-1;
                long newId = dbTrack.addTrack(tracks.get(currentTrackID));
                tracks.get(currentTrackID).setId(newId);
                addTrackFlag = true; // tracking icon is active
                clickedTrack = -1; // hide balloon popups
                //newTrack = false;

                showTracks = true;
                action_showTracks.setChecked(true);
                trackMenuItem.setIcon(R.drawable.ic_cyan_track);
                Toast.makeText(PDFActivity.this, getResources().getString(R.string.trackingOn), Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.action_add_way_pt_menu) {
                addWayPtFlag = true;
                clickedWP = -1; // hide balloon popups
                newWP = false;
                showAllWayPts = true;
                action_showWayPts.setChecked(true);
                wayPtMenuItem.setIcon(R.mipmap.ic_cyan_pin_forgnd);
                Toast.makeText(PDFActivity.this, getResources().getString(R.string.wayPtInstr), Toast.LENGTH_LONG).show();
        }
        else if (id == R.id.action_add_lat_long_menu) {
            // Add Waypoint by Lat/Long
            txtLatLong = new EditText(this);
            txtLatLong.setPadding(40,0,0,40);
            txtLatLong.setHint("lat, long");
            txtLatLong.setInputType(EditorInfo.TYPE_CLASS_TEXT);

            AlertDialog.Builder builderLatLong = new AlertDialog.Builder(PDFActivity.this)
                .setTitle("Add Waypoint by Lat/Long")
                .setMessage("Enter a latitude between "+String.format(Locale.US, "%.4f",lat1)+" and "+String.format(Locale.US, "%.4f",lat2)+" and longitude between "+String.format(Locale.US, "%.4f",long1)+" and "+String.format(Locale.US, "%.4f",long2)+".\n\nExample: "+String.format(Locale.US,"%.4f",(lat2+lat1)/2.0)+","+String.format(Locale.US,"%.4f",(long2+long1)/2.0)+"\n\n")
                .setView(txtLatLong)
                .setPositiveButton("Add", (dialog, whichButton) -> {
                })
                .setNegativeButton("Cancel", (dialog, whichButton) -> {
                });

            final AlertDialog dialogLatLong = builderLatLong.create();
            dialogLatLong.show();
            // overriding the handler immediately after show so that it does not call dismiss
            dialogLatLong.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String[] latlong = txtLatLong.getText().toString().split(",");
                if (latlong.length != 2){
                    Toast.makeText(PDFActivity.this,"Missing comma. Enter: lat, long", Toast.LENGTH_LONG).show();
                }
                else{
                // Add waypoint
                try {
                    double latitude = Double.parseDouble(latlong[0]);
                    double longitude = Double.parseDouble(latlong[1]);
                    if (longitude > 0) longitude = longitude * -1;// make longitude negative
                    if (latitude <= lat1 || latitude >= lat2) {
                        Toast.makeText(PDFActivity.this, "latitude is not on map!!!!", Toast.LENGTH_LONG).show();
                    } else if (longitude <= long1 || longitude >= long2) {
                        Toast.makeText(PDFActivity.this, "longitude is not on map!!!!", Toast.LENGTH_LONG).show();
                    } else {
                        addWayPtFlag = false;
                        clickedWP = -1; // hide balloon popups
                        showAllWayPts = true;
                        action_showWayPts.setChecked(true);
                        wait.setVisibility(View.VISIBLE);
                        newWP = true;
                        String location = String.format(Locale.US, "%.5f, %.5f", latitude, longitude);
                        int num = findAUniqueName();
                        WayPt wayPt = wayPts.add(mapName, "Waypoint " + num, (float) longitude, (float) latitude, "blue", location);
                        wayPts.SortPts();
                        try {
                            db.addWayPt(wayPt);
                            // get the index of the new waypoint
                            for (int i1 = 0; i1 < wayPts.size(); i1++) {
                                if (wayPts.get(i1).getX() == (float) longitude && wayPts.get(i1).getY() == (float) latitude) {
                                    lastClickedWP = clickedWP;
                                    clickedWP = i1;
                                    break;
                                }
                            }
                        } catch (SQLException exc) {
                            Toast.makeText(PDFActivity.this, "Failed to save waypoint. " + exc.getMessage(), Toast.LENGTH_LONG).show();
                            wayPts.remove((float) longitude, (float) latitude);
                            clickedWP = -1;
                            newWP = false;
                            addWayPtFlag = false;
                        }
                        dialogLatLong.dismiss();
                    }
                }catch (NumberFormatException e){
                    Toast.makeText(PDFActivity.this,"lat, long must be numbers in decimal degrees", Toast.LENGTH_LONG).show();
                }
            }
            });
        }
        else if (id == R.id.action_tracking_settings) {
            // Trigger our floating drop-down menu panel
            showTrackingConfigurationPanel();
            return true;
        }
        else if (id == R.id.action_help){
            Intent i = new Intent(PDFActivity.this, PDFHelpActivity.class);
            startActivity(i);
        }
        else if (item.getItemId() == android.R.id.home){
            // back button pressed, return
            // stop tracking
            turnTrackingOff();
            Intent mainIntent = new Intent(PDFActivity.this, MainActivity.class);
            mainIntent.putExtra("ID",id);
            mainIntent.putExtra("UPDATES", "true");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showTrackingConfigurationPanel() {
        // 1. Inflate the layout manually
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.menu_tracking_config, null);

        // 2. Initialize the views inside the layout
        TextView intervalLabel = popupView.findViewById(R.id.menu_interval_label);
        SeekBar seekBar = popupView.findViewById(R.id.menu_interval_seekbar);
        Button btnAuto = popupView.findViewById(R.id.menu_btn_reset_auto);

        // 3. Create the window frame configuration
        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // Allows closing the panel if the user taps outside of it
        );

        // 4. Bind the SeekBar listener logic
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) progress = 1;
                intervalLabel.setText("Manual Interval: " + progress + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (progress < 1) progress = 1;

                Intent intent = new Intent(PDFActivity.this, TrackingService.class);
                intent.putExtra("update_interval", progress * 1000L);
                intent.putExtra("update_fastest_interval", (progress * 1000L) / 2);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                // Closes the drop-down menu once auto-speed is toggled
                popupWindow.dismiss();
            }
        });

        // 5. Bind the Auto Button click listener
        btnAuto.setOnClickListener(v -> {
            Intent intent = new Intent(PDFActivity.this, TrackingService.class);
            intent.putExtra("enable_auto", true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            // Closes the drop-down menu once auto-speed is toggled
            popupWindow.dismiss();
        });

        // 6. Anchor the window framework underneath the top right menu zone
        View anchorView = findViewById(android.R.id.content);
        if (anchorView != null) {
            // Displays the custom overlay in the upper right quadrant of the display screen
            popupWindow.showAtLocation(anchorView, Gravity.TOP | Gravity.END, 16, 150);
        }
    }

    // Turn tracking odd
    private void turnTrackingOff(){
        addTrackFlag = false; // tracking icon in top menu is inactive
        trackMenuItem.setIcon(R.drawable.ic_gray_track); // set to gray track
        currentTrackID = -1;
        clickedTrack = -1;
        Toast.makeText(PDFActivity.this, getResources().getString(R.string.trackingOff), Toast.LENGTH_LONG).show();
    }
    // ADJUST WAYPOINT MENU
    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.move_menu, menu);
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
            // Move pin
            if (item.getItemId() == R.id.move_here){
                float x = adjustX - pdfView.getCurrentXOffset();
                float y = adjustY - pdfView.getCurrentYOffset();
                float zoom = pdfView.getZoom();
                double toScreenCordX = (optimalPageWidth.get() * zoom) / mediaBoxWidth;
                double toScreenCordY = (optimalPageHeight.get() * zoom) / mediaBoxHeight;
                double marginL = toScreenCordX * marginLeft;
                double marginT = toScreenCordY * marginTop;
                double marginx = toScreenCordX * marginXworld;
                double marginy = toScreenCordY * marginYworld;
                double longitude = (((x - marginL) / ((optimalPageWidth.get() * zoom) - marginx)) * longDiff) + long1;
                double latitude = ((((y - marginT) / ((optimalPageHeight.get() * zoom) - marginy)) * latDiff) - lat2) * -1;
                if (!(latitude > lat1 && latitude < lat2 && longitude > long1 && longitude < long2)) {
                    Toast.makeText(PDFActivity.this, getString(R.string.offMap), Toast.LENGTH_LONG).show();
                    mode.finish(); //hide menu
                    return false;
                }
                WayPt wayPt = wayPts.get(adjustWP);
                wayPt.setX((float) longitude);
                wayPt.setY((float) latitude);
                String location = String.format(Locale.US,"%.5f, %.5f", latitude,longitude);
                wayPt.setLocation(location);
                db.updateWayPt(wayPt);
                mode.finish(); //hide menu
                return false;
            }
            // Cancel
            else if (item.getItemId() == R.id.cancel) {
                mode.finish();
                return false;
            }
            else {
                mode.finish(); // Action picked, so close the CAB
                return false;
            }
        }

        // Called when the user exits the action mode by clicking back arrow or back button
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adjustWP = -1; // start drawing waypoint pin again
            moveIcon.setVisibility(View.GONE);
            mActionMode = null;
            PDFActivity.this.setTitle(mapName);
            TextView note = findViewById(R.id.move_instr);
            note.setVisibility(View.GONE);
            pdfView.invalidate();
        }
    };

    // Delete Waypoint
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'DELETE' button clicked, remove waypoint or track
                    // delete waypoint
                    WayPt wayPt = wayPts.get(del_id);
                    db.deleteWayPt(wayPt);
                    wayPts.remove(wayPt.getX(), wayPt.getY());
                    deleting = false;
                    pdfView.invalidate();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'CANCEL' button clicked, do nothing
                    deleting = false;
                    break;
            }
        }
    };

    // Delete Track
    DialogInterface.OnClickListener trackDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'DELETE' button clicked, remove waypoint or track
                    // delete track
                    Track track = tracks.get(del_id);
                    dbTrack.deleteTrack(track);
                    tracks.remove(track);
                    deleting = false;
                    clickedTrack = -1;
                    pdfView.invalidate();
                    if (del_id == currentTrackID) {
                        turnTrackingOff();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'CANCEL' button clicked, do nothing
                    deleting = false;
                    clickedTrack = -1;
                    break;
            }
        }
    };

    // Delete All Way Points
    DialogInterface.OnClickListener dialogAllClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'DELETE' button clicked, remove map from imported maps
                    db.deleteWayPts(mapName);
                    wayPts.removeAll();
                    deleting = false;
                    pdfView.invalidate();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'CANCEL' button clicked, do nothing
                    deleting = false;
                    break;
            }
        }
    };
}
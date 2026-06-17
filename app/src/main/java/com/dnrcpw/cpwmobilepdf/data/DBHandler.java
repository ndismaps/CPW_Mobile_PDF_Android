package com.dnrcpw.cpwmobilepdf.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.model.PDFMap;
import com.dnrcpw.cpwmobilepdf.model.Track;
import com.dnrcpw.cpwmobilepdf.model.Tracks;
import com.dnrcpw.cpwmobilepdf.model.WayPt;
import com.dnrcpw.cpwmobilepdf.model.WayPts;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by tammy on 12/6/2017.
 */

public class DBHandler extends SQLiteOpenHelper {
    private static DBHandler sInstance; // used by every activity. Application context.
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "mapsInfo";
    // Maps table name
    private static final String TABLE_MAPS = "maps";
    // Maps Table Column Names
    private static final String KEY_ID = "id";
    private static final String KEY_PATH = "path";
    private static final String KEY_BOUNDS = "bounds";
    private static final String KEY_MEDIABOX = "mediabox";
    private static final String KEY_VIEWPORT = "viewport";
    private static final String KEY_THUMBNAIL = "thumbnail";
    private static final String KEY_NAME = "name";
    private static final String KEY_FILESIZE = "filesize";
    private static final String KEY_DISTTOMAP = "disttomap";
    private static final String KEY_MAP_ORIENTATION = "orientation";

    // Settings table name. Store user preferred settings here
    private static final String TABLE_SETTINGS = "settings";
    // Settings table column names
    private static final String KEY_SETTINGS_ID = "id";
    private static final String KEY_MAP_SORT = "map_sort"; // Imported maps sort order. Valid values: name, date, or size
    private static final String KEY_LOAD_ADJ_MAPS ="load_adj_maps"; // turn on or off loading of adjacent maps for all maps. Valid values: "1" or "0"
    private static final String KEY_SHOW_WAYPOINTS="show_waypoints"; // turn on or off showing waypoints for all maps. Valid values: "1" or "0"
    private static final String KEY_SHOW_TRACKS="show_tracks"; // turn on or off showing tracks for all maps. Valid values: "1" or "0"
    private static final String KEY_SHOW_ALL_WAYPOINT_LABELS="show_all_waypoints"; //  turn on or off showing all waypoint labels for all maps. Valid values: "1" or "0"

    // Tracks table name. Stores tracks for each map
    private static final String TABLE_TRACKS = "tracks";
    // Tracks Table Columns names
    private static final String KEY_MAPNAME = "mapname";
    private static final String KEY_DESC = "descrption";
    private static final String KEY_LINE_SEGMENTS = "linesegments"; // comma delimited x,y pairs of long, lat
    private static final String KEY_COLOR = "color";
    private static final String KEY_TIME = "time";

    // Waypoint table name. Stores waypoints for each map
    private static final String TABLE_WAYPTS = "wayPts";
    // wayPts Table Columns names
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_LOCATION = "location";


    public static synchronized DBHandler getInstance(Context context) throws SQLException {
        // 5-18-26 Add sInstance
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DBHandler(context.getApplicationContext());
            //context = context;
        }
        return sInstance;
    }
    private final Context context;
    public DBHandler(Context c) throws SQLException {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = c;
    }

    @Override
    public void onCreate(SQLiteDatabase db1) throws SQLException {
        // This only runs for BRAND NEW installs who don't have any database yet
        createMapsTable(db1); // Create Imported Maps table
        createSettingsTable(db1); // Create User Settings table
        createTracksTable(db1); // Create Tracks table
        createWayPtTable(db1); // Create Waypoints table
    }

    @Override
    public void onUpgrade(SQLiteDatabase db1, int oldVersion, int newVersion) throws SQLException {
         // For new version do new stuff here. Drop tables
         if (oldVersion != newVersion) {
             switch (oldVersion) {
                 case 1:
                     // Forgot to update the version number so handle it here.
                     String selectQuery = "SELECT * FROM " + TABLE_MAPS;
                     Cursor cursor = db1.rawQuery(selectQuery, null);
                     if (cursor.getColumnCount() == 7){
                         if (cursor != null) {
                             cursor.close();
                         }
                         db1.execSQL("ALTER TABLE " + TABLE_MAPS + " ADD COLUMN " + KEY_FILESIZE + " TEXT");
                         db1.execSQL("ALTER TABLE " + TABLE_MAPS + " ADD COLUMN " + KEY_DISTTOMAP + " TEXT");
                         db1.execSQL("UPDATE " + TABLE_MAPS + " SET " + KEY_FILESIZE + " = ''");
                         db1.execSQL("UPDATE " + TABLE_MAPS + " SET " + KEY_DISTTOMAP + " = ''");
                     }
                     // Version 2 new stuff
                     db1.execSQL("ALTER TABLE " + TABLE_MAPS + " ADD COLUMN " + KEY_MAP_ORIENTATION + " TEXT");
                     db1.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + KEY_LOAD_ADJ_MAPS + " TEXT");
                     db1.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + KEY_SHOW_WAYPOINTS + " TEXT");
                     db1.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + KEY_SHOW_ALL_WAYPOINT_LABELS + " TEXT");
                     db1.execSQL("UPDATE " + TABLE_MAPS + " SET " + KEY_MAP_ORIENTATION + " = 'none'");
                     db1.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_LOAD_ADJ_MAPS + " = '1'");
                     db1.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_SHOW_WAYPOINTS + " = '1'");
                     db1.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_SHOW_ALL_WAYPOINT_LABELS + " = '0'");
                     // version 3 new stuff
                     db1.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + KEY_SHOW_TRACKS + " TEXT");
                     db1.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_SHOW_TRACKS + " = '0'");
                     // Version 4 new stuff
                     // Create Tables if they don't exist
                     createWayPtTable(db1);
                     createTracksTable(db1);
                     // Migrate data from the other two standalone files into this open 'db1' instance
                     migrateExternalDatabase(context, db1, "wayPtsInfo", "wayPts");
                     migrateExternalDatabase(context, db1, "tracksInfo", "tracks");
                 case 2:
                     // Version 3 new stuff
                     db1.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + KEY_SHOW_TRACKS + " TEXT");
                     db1.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_SHOW_TRACKS + " = '0'");
                     // Version 4 new stuff
                     // Create Tables if they don't exist
                     createWayPtTable(db1);
                     createTracksTable(db1);
                     // Migrate data from the other two standalone files into this open 'db1' instance
                     migrateExternalDatabase(context, db1, "wayPtsInfo", "wayPts");
                     migrateExternalDatabase(context, db1, "tracksInfo", "tracks");
                 case 3:
                     // Version 4 new stuff
                     // Create Tables if they don't exist
                     createWayPtTable(db1);
                     createTracksTable(db1);
                     // Migrate data from the other two standalone files into this open 'db1' instance
                     migrateExternalDatabase(context, db1, "wayPtsInfo", "wayPts");
                     migrateExternalDatabase(context, db1, "tracksInfo", "tracks");
             }
        }
    }

    private void migrateExternalDatabase(Context context, SQLiteDatabase currentDb, String oldDbName, String tableName) {
        File oldDbFile = context.getDatabasePath(oldDbName);

        // Safety check: If the old file doesn't exist, skip it
        if (!oldDbFile.exists()) {
            return;
        }

        SQLiteDatabase oldDb = null;
        Cursor cursor = null;

        try {
            // Open the old database file in read-only mode
            oldDb = SQLiteDatabase.openDatabase(oldDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            cursor = oldDb.query(tableName, null, null, null, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ContentValues cv = new ContentValues();

                    // Dynamically map all columns for this row from old DB to new DB
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        String columnName = cursor.getColumnName(i);
                        switch (cursor.getType(i)) {
                            case Cursor.FIELD_TYPE_INTEGER:
                                cv.put(columnName, cursor.getLong(i));
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                cv.put(columnName, cursor.getDouble(i));
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                cv.put(columnName, cursor.getString(i));
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                cv.put(columnName, cursor.getBlob(i));
                                break;
                        }
                    }
                    // Insert the row directly into the current unified database file
                    currentDb.insert(tableName, null, cv);
                }
            }

            // Step C: Delete the old file from the disk now that data is copied
            context.deleteDatabase(oldDbName);

        } catch (Exception e) {
            Log.e("MIGRATION_ERROR", "Failed migrating " + oldDbName, e);
        } finally {
            if (cursor != null) cursor.close();
            if (oldDb != null) oldDb.close();
        }
    }


    //-----------------------------------
    // MAPS TABLE (details for each map)
    //-----------------------------------
    private void createMapsTable (SQLiteDatabase db1) throws SQLException {
        // Create Imported Maps Table
        String CREATE_MAPS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MAPS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PATH + " TEXT,"
                + KEY_BOUNDS + " TEXT," + KEY_MEDIABOX + " TEXT,"
                + KEY_VIEWPORT + " TEXT, " + KEY_THUMBNAIL + " BLOB,"
                + KEY_NAME + " TEXT," + KEY_FILESIZE + " TEXT,"
                + KEY_DISTTOMAP + " TEXT," + KEY_MAP_ORIENTATION + " TEXT)";
        db1.execSQL(CREATE_MAPS_TABLE);
    }

    public void deleteMapsTable(SQLiteDatabase db1) throws SQLException {
        // Delete and recreate Table_Maps
        db1.execSQL("DROP TABLE IF EXISTS " + TABLE_MAPS);
        createMapsTable(db1);
    }

    // Adding new PDF Map
    public Integer addMap(PDFMap map) throws SQLiteException {
        SQLiteDatabase db = this.getWritableDatabase();
        return addMapToMapsTable(db, map);
    }

    private Integer addMapToMapsTable(SQLiteDatabase db1, PDFMap map) {
        ContentValues values = new ContentValues();
        values.put(KEY_PATH, map.getPath()); // Path and file name of map
        values.put(KEY_BOUNDS, map.getBounds()); // Lat/Long Bounds of the map
        values.put(KEY_MEDIABOX, map.getMediabox()); // Pixel Bounds of the map
        values.put(KEY_VIEWPORT, map.getViewport()); // Margins
        values.put(KEY_THUMBNAIL, map.getThumbnail()); // Thumbnail image
        values.put(KEY_NAME, map.getName()); // Map name without path
        values.put(KEY_FILESIZE, map.getFileSize()); // Map pdf file size 267 Kb
        values.put(KEY_DISTTOMAP, map.getDistToMap()); // Current distance to map
        values.put(KEY_MAP_ORIENTATION, map.getMapOrientation()); // lock map in certain orientation? none, portrait, landscape
        // Inserting Row
        long index = db1.insert(TABLE_MAPS, null, values);
        return (int) index;
    }

    public PDFMap getMap(String mapName) throws  SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_MAPS, new String[]{KEY_ID, KEY_PATH, KEY_BOUNDS, KEY_MEDIABOX, KEY_VIEWPORT, KEY_THUMBNAIL, KEY_NAME, KEY_FILESIZE, KEY_DISTTOMAP, KEY_MAP_ORIENTATION}, KEY_NAME + "=?",
                new String[]{mapName}, null, null, null, null);
        if (cursor.moveToFirst()){
            PDFMap map = new PDFMap(cursor.getString(1), cursor.getString(2), cursor.getString(3),
                    cursor.getString(4),cursor.getString(5), cursor.getString(6),
                    cursor.getString (7), cursor.getString(8),cursor.getString(9));
            map.setId(Integer.parseInt(cursor.getString(0)));
            if (cursor != null) {
                cursor.close();
            }
            return map;
        }
        return null;
    }

    // Getting All PDF Maps
    public ArrayList<PDFMap> getAllMaps() throws SQLiteException {
        // Called by CustomAdapter creation
        SQLiteDatabase db = this.getWritableDatabase();

        ArrayList<PDFMap> mapList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MAPS;
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                PDFMap map = new PDFMap();
                map.setId(Integer.parseInt(cursor.getString(0)));
                map.setPath(cursor.getString(1));
                map.setBounds(cursor.getString(2));
                map.setMediabox(cursor.getString(3));
                map.setViewport(cursor.getString(4));
                // thumbnail was saved to a file, get the path
                map.setThumbnail(cursor.getString(5));
                map.setName(cursor.getString(6));
                map.setFileSize(cursor.getString(7));
                if (map.getFileSize().equals("")) {
                    try {
                        File file = new File(map.getPath());
                        String fileSize;
                        long size = file.length() / 1024; // Get size and convert bytes into Kb.
                        if (size >= 1024) {
                            double sizeDbl = (double) size;
                            fileSize = String.format(Locale.US, "%.1f%s", (sizeDbl / 1024), context.getResources().getString(R.string.Mb));
                        } else {
                            fileSize = size + context.getResources().getString(R.string.Kb);
                        }
                        map.setFileSize(fileSize);
                    }catch (NullPointerException e){
                        map.setFileSize("");
                    }
                }
                map.setDistToMap(cursor.getString(8));
                if (map.getDistToMap().isEmpty()) {
                    map.setMiles(0.0);
                } else {
                    try {
                        map.setMiles(Double.parseDouble(map.getDistToMap()));
                    } catch (NumberFormatException e) {
                        map.setMiles(-999.99);
                    }
                }
                map.setMapOrientation(cursor.getString(9));
                // Adding map to list
                mapList.add(map);
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        // return map list
        return mapList;
    }

    // Updating a PDF Map
    public void updateMap(PDFMap map) throws SQLiteException {
        // update a map in the database
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATH, map.getPath());
        values.put(KEY_BOUNDS, map.getBounds());
        values.put(KEY_MEDIABOX, map.getMediabox());
        values.put(KEY_VIEWPORT, map.getViewport());
        values.put(KEY_THUMBNAIL, map.getThumbnail());
        values.put(KEY_NAME, map.getName());
        values.put(KEY_FILESIZE, map.getFileSize());
        values.put(KEY_DISTTOMAP, map.getDistToMap());
        values.put(KEY_MAP_ORIENTATION, map.getMapOrientation());

        // updating row
        db.update(TABLE_MAPS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(map.getId())});
    }

    // Deleting a PDF Map
    public void deleteMap(PDFMap map) throws SQLiteException{
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MAPS, KEY_ID + " = ?",
                new String[]{String.valueOf(map.getId())});
    }


    //------------------------------------
    //  SETTINGS TABLE
    //  (user preferences application wide)
    //-------------------------------------

    public void createSettingsTable(SQLiteDatabase db1) throws SQLException{
        // User preferences for all maps
        String CREATE_SETTINGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + "("
                + KEY_SETTINGS_ID + " INTEGER PRIMARY KEY," + KEY_MAP_SORT + " TEXT," + KEY_LOAD_ADJ_MAPS + " TEXT,"
                + KEY_SHOW_WAYPOINTS + " TEXT," + KEY_SHOW_ALL_WAYPOINT_LABELS + " TEXT," + KEY_SHOW_TRACKS + " TEXT)";
        db1.execSQL(CREATE_SETTINGS_TABLE);
        // Insert default user settings
        ContentValues values = new ContentValues();
        values.put(KEY_MAP_SORT, "date");
        values.put(KEY_LOAD_ADJ_MAPS, "1");
        values.put(KEY_SHOW_WAYPOINTS, "1");
        values.put(KEY_SHOW_ALL_WAYPOINT_LABELS, "0");
        values.put(KEY_SHOW_TRACKS, "1");
        db1.insert(TABLE_SETTINGS, null, values);
    }

    // Load Adjacent Maps?
    public void setLoadAdjMaps(int load) throws SQLiteException{
        // Sets user preference, should load adjacent maps if current location goes off the map and onto another map?
        // Displays a dropdown menu of maps to choose from. This is a checkbox on the maps more menu
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LOAD_ADJ_MAPS, Integer.toString(load));
        String id = "1";
        db.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
    }
    public int getLoadAdjMaps() throws SQLiteException {
        // Sets user preference, should load adjacent maps if current location goes off the map and onto another map?
        // Displays a dropdown menu of maps to choose from. This is a checkbox on the maps more menu
        SQLiteDatabase db = this.getWritableDatabase();
        int load_adj_maps;
        String selectQuery = "SELECT " + KEY_LOAD_ADJ_MAPS + " FROM " + TABLE_SETTINGS;
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Only one record
        if (cursor.moveToFirst()) {
            load_adj_maps = Integer.parseInt(cursor.getString(0));
            if (cursor != null) {
                cursor.close();
            }
            return load_adj_maps;
        }
        else {
            // Settings table does not exist. Create it.
            createSettingsTable(db);
            return 1;
        }
    }
    //---------------------
    // Show Tracks?
    //---------------------
    public void setShowTracks(int show) throws SQLiteException{
        // Sets user preference, should show waypoints when map loads?
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SHOW_TRACKS, Integer.toString(show));
        String id = "1";
        db.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
    }
    public int getShowTracks() throws SQLiteException {
        // Sets user preference, should show tracks when map loads?
        SQLiteDatabase db = this.getWritableDatabase();
        int show;
        String selectQuery = "SELECT " + KEY_SHOW_TRACKS + " FROM " + TABLE_SETTINGS;
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Only one record
        if (cursor.moveToFirst()) {
            show = Integer.parseInt(cursor.getString(0));
            if (cursor != null) {
                cursor.close();
            }
            return show;
        }
        else {
            // Settings table does not exist. Create it.
            createSettingsTable(db);
            return 0;
        }
    }
    //---------------------
    // Show Waypoints?
    //---------------------
    public void setShowWaypoints(int show) throws SQLiteException{
        // Sets user preference, should show waypoints when map loads?
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SHOW_WAYPOINTS, Integer.toString(show));
        String id = "1";
        db.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
    }
    public int getShowWaypoints() throws SQLiteException {
        // Sets user preference, should show all waypoints when map loads?
        SQLiteDatabase db = this.getWritableDatabase();
        int show;
        String selectQuery = "SELECT " + KEY_SHOW_WAYPOINTS + " FROM " + TABLE_SETTINGS;
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Only one record
        if (cursor.moveToFirst()) {
            show = Integer.parseInt(cursor.getString(0));
            if (cursor != null) {
                cursor.close();
            }
            return show;
        }
        else {
            // Settings table does not exist. Create it.
            createSettingsTable(db);
            return 1;
        }
    }
    //---------------------
    // Show All Waypoint Labels?
    //---------------------
    public void setShowAllWaypointLabels(int show) throws SQLiteException{
        // Sets user preference, should show waypoint labels when map loads?
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SHOW_ALL_WAYPOINT_LABELS, Integer.toString(show));
        String id = "1";
        db.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
    }
    public int getShowAllWaypointLabels() throws SQLiteException {
        // Sets user preference, should show all waypoint labels when map loads?
        SQLiteDatabase db = this.getWritableDatabase();
        int show;
        String selectQuery = "SELECT " + KEY_SHOW_ALL_WAYPOINT_LABELS + " FROM " + TABLE_SETTINGS;
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Only one record
        if (cursor.moveToFirst()) {
            show = Integer.parseInt(cursor.getString(0));
            if (cursor != null) {
                cursor.close();
            }
            return show;
        }
        else {
            // Settings table does not exist. Create it.
            createSettingsTable(db);
            return 0;
        }
    }
    //---------------
    //     SORTING
    //---------------
    public void setMapSort(String order) throws SQLiteException{
        // How to sort the MainActivity imported maps
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAP_SORT, order);
        String id = "1";
        db.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
    }

    public String getMapSort() throws SQLiteException {
        // How to sort the MainActivity imported maps
        SQLiteDatabase db = this.getWritableDatabase();
        String order;
        String selectQuery = "SELECT " + KEY_MAP_SORT + " FROM " + TABLE_SETTINGS;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Only one record
        if (cursor.moveToFirst()) {
            order = cursor.getString(0);
            if (cursor != null) {
                cursor.close();
            }
            return order;
        }
        else {
            createSettingsTable(db);
            return "date"; // default value
        }
    }


    //--------------------------
    // Tracks Table
    //--------------------------
    private void createTracksTable(SQLiteDatabase db1) throws SQLException {
        String CREATE_TRACKS_TABLE = "CREATE TABLE " + TABLE_TRACKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_MAPNAME + " TEXT, "
                + KEY_DESC + " TEXT, " + KEY_LINE_SEGMENTS + " TEXT, "
                + KEY_COLOR + " TEXT, " + KEY_TIME + " TEXT)";
        db1.execSQL(CREATE_TRACKS_TABLE);
    }

    public synchronized void updateTrack(double latitude, double longitude, long currentDBId){
        // save the line segments when app is in the background and foreground
        // Called by TrackingService
        String lineSegments = "";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_TRACKS, new String[]{KEY_LINE_SEGMENTS}, KEY_ID + "=?",
                new String[]{String.valueOf(currentDBId)}, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                lineSegments = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LINE_SEGMENTS));
            }
            cursor.close(); // Crucial to close cursor to avoid memory leaks
        }
        // Make sure they have moved
        if (!lineSegments.isEmpty()) {
            // Get the last lat long, then compare if distance is greater than 10 meters
            int pos = lineSegments.lastIndexOf(",");
            String str = lineSegments.substring(pos+1);
            double lastLat = Double.parseDouble(str);
            // remove last latitude
            str = lineSegments.substring(0,pos);
            pos = str.lastIndexOf(",");
            double lastLong;
            // test for only one point, no comma
            if (pos == -1)
                lastLong = Double.parseDouble(str);
            else
                lastLong = Double.parseDouble(str.substring(pos+1));
            // If the distance has not changed more than 5 meters, don't record the track segment
            Log.d("distance","distance between lat,long in m="+calculateDistance(lastLat,lastLong,latitude,longitude));
            if (calculateDistance(lastLat,lastLong,latitude,longitude) < 5) return;
        }
        if (!lineSegments.isEmpty()) lineSegments += ",";
        lineSegments = lineSegments+longitude+","+latitude;

        ContentValues values = new ContentValues();
        values.put(KEY_LINE_SEGMENTS, lineSegments); // String of x1,y1,x2,y2,x3,y3,... line segments in long, lat

        // Update line segments
        db.update(TABLE_TRACKS, values,KEY_ID + " = ?",
                new String[]{ String.valueOf(currentDBId) });
        //db.close(); // is this needed????????????????????????
    }
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // returns the distance between 2 lat,long points in meters
        double EARTH_RADIUS = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Returns distance in meters
    }
    public long addTrack(Track track) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAPNAME, track.getMapName()); // Name of map
        values.put(KEY_DESC, track.getDesc()); // Track description
        values.put(KEY_LINE_SEGMENTS, track.getLineSegments()); // String of x1,y1,x2,y2,x3,y3,... line segments in long, lat
        values.put(KEY_COLOR, track.getColorName()); // Color name of pushpin image
        values.put(KEY_TIME, track.getTime()); // Date and time of creation of track
        // Inserting Row
        return db.insert(TABLE_TRACKS, null, values);
    }

    public int updateTrack(Track track){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAPNAME, track.getMapName()); // Name of map
        values.put(KEY_DESC, track.getDesc()); // Track description
        values.put(KEY_LINE_SEGMENTS, track.getLineSegments()); // String of x1,y1,x2,y2,x3,y3,... line segments in long, lat
        values.put(KEY_COLOR, track.getColorName()); // Color name of pushpin image
        values.put(KEY_TIME, track.getTime()); // Date and time of creation of track
        // updating row
        return db.update(TABLE_TRACKS, values, KEY_ID + " = ?",
                new String[]{ String.valueOf(track.getId()) });
    }

    // Delete all Tracks for a given PDF map
    public void deleteTracks(String mapName) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(TABLE_TRACKS, "mapName=?", new String[]{mapName});
    }
    // Deleting a Track by track class
    public void deleteTrack(Track track) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACKS, KEY_ID + " = ?",
                new String[] { String.valueOf(track.getId()) });
    }

    // Deleting a Track by track id
    public void deleteTrack(int id) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACKS, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }

    // Get all tracks for a given map
    public Tracks getTracks(String mapName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Tracks trackList = new Tracks();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_TRACKS;
        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<Integer> deleteIds = new ArrayList<>();

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                // Make sure this track has line segments and a mapName
                if (cursor.getString(3).isEmpty() || cursor.getString(1).isEmpty())
                    deleteIds.add(cursor.getInt(0));
                // Adding track to list if matches name
                else if (mapName.equals(cursor.getString(1))) {
                    trackList.add(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        // Delete empty tracks with no line segments
        for (int i=0; i<deleteIds.size(); i++)
            deleteTrack(deleteIds.get(i));
        // return tracks object
        return trackList;
    }


    //---------------------------
    // Waypoint Table
    //---------------------------
    private void createWayPtTable(SQLiteDatabase db1) throws SQLException {
        String CREATE_WAYPTS_TABLE = "CREATE TABLE " + TABLE_WAYPTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_MAPNAME + " TEXT, "
                + KEY_DESC + " TEXT, " + KEY_X + " FLOAT, "
                + KEY_Y + " FLOAT, " +KEY_COLOR + " TEXT, " + KEY_TIME + " TEXT, "
                + KEY_LOCATION + " TEXT"+")";
        db1.execSQL(CREATE_WAYPTS_TABLE);
    }

    /*public void deleteTable(Context c){
        SQLiteDatabase db = this.getWritableDatabase();
        // delete maps table
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_WAYPTS);
        // Create maps table again
        onCreate(db);
        Toast.makeText(c, "All imported waypoints were deleted.", Toast.LENGTH_LONG).show();
    }*/

    // Adding waypoint
    public void addWayPt(WayPt wayPt) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAPNAME, wayPt.getName()); // Name of map
        values.put(KEY_DESC, wayPt.getDesc()); // Waypoint description
        values.put(KEY_X, wayPt.getX()); // x screen coordinate
        values.put(KEY_Y, wayPt.getY()); // y screen coordinate
        values.put(KEY_COLOR, wayPt.getColorName()); // Color name of pushpin image
        values.put(KEY_TIME, wayPt.getTime()); // Date and time of creation of waypoint
        values.put(KEY_LOCATION, wayPt.getLocation()); // Lat, Long
        // Inserting Row
        db.insert(TABLE_WAYPTS, null, values);
    }

    // Delete all WayPts for a given PDF map
    public void deleteWayPts(String mapName) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(TABLE_WAYPTS, "mapName=?", new String[]{mapName});
    }

    // Getting one WayPt
    /*public WayPt getWayPt(int id) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WAYPTS, new String[]{KEY_ID,
                        KEY_MAPNAME, KEY_DESC, KEY_X, KEY_Y, KEY_COLOR, KEY_TIME, KEY_LOCATION}, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        WayPt wayPt;
        try {
            wayPt = new WayPt(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2), cursor.getFloat(3), cursor.getFloat(4),
                    cursor.getString(5), cursor.getString(6), cursor.getString(7));
        } catch (NullPointerException e) {
            Toast.makeText(c, "Error reading database.", Toast.LENGTH_LONG).show();
            cursor.close();
            return null;
        }
        cursor.close();
        return wayPt;
    }*/

    // Get All MapNames
    public ArrayList<String> getAllMapNames() throws SQLException {
        // 4-16-26 for removing waypts that no longer have an existing map. Old bug.
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            ArrayList<String> list = new ArrayList<>();
            // Select All Query
            String selectQuery = "SELECT * FROM " + TABLE_WAYPTS;
            Cursor cursor = db.rawQuery(selectQuery, null);

            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    // Add mapName to list
                    if (!list.contains(cursor.getString(1)))
                        list.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }
            cursor.close();
            // return map names list
            return list;
        }catch(SQLException e){
            throw new SQLException(e.getMessage());
        }
    }

    // Getting All Waypoints from one PDF map
    public WayPts getWayPts(String mapName) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        WayPts wayPtsList = new WayPts(mapName);
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_WAYPTS;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                // Adding waypoint to list if matches name
                if (mapName.equals(cursor.getString(1)))
                    wayPtsList.add(Integer.parseInt(cursor.getString(0)),cursor.getString(1),cursor.getString(2),cursor.getFloat(3),cursor.getFloat(4),cursor.getString(5),cursor.getString(6),cursor.getString(7));

            } while (cursor.moveToNext());
        }
        cursor.close();
        // return waypoints list
        return wayPtsList;
    }

    // Updating a Waypoint
    public int updateWayPt(WayPt wayPt) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAPNAME, wayPt.getName());
        values.put(KEY_DESC, wayPt.getDesc());
        values.put(KEY_X, wayPt.getX());
        values.put(KEY_Y, wayPt.getY());
        values.put(KEY_COLOR, wayPt.getColorName());
        values.put(KEY_TIME, wayPt.getTime());
        values.put(KEY_LOCATION, wayPt.getLocation());

        // updating row
        return db.update(TABLE_WAYPTS, values, KEY_ID + " = ?",
                new String[]{ String.valueOf(wayPt.getId()) });
    }

    // Deleting a Waypoint
    public void deleteWayPt(WayPt wayPt) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WAYPTS, KEY_ID + " = ?",
                new String[] { String.valueOf(wayPt.getId()) });
    }

    // Deleting a Waypoint
    public void deleteWayPt(String mapName) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WAYPTS, KEY_MAPNAME + " = ?",
                new String[]{mapName});
    }

}
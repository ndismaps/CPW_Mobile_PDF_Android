package com.dnrcpw.cpwmobilepdf.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.model.PDFMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by tammy on 12/6/2017.
 */

public class DBHandler extends SQLiteOpenHelper {
    private static DBHandler sInstance; // used by every activity. Application context.
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "mapsInfo";
    private final Context context = null;
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

    public static synchronized DBHandler getInstance(Context context) throws SQLException {
        // 5-18-26 Add sInstance
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DBHandler(context.getApplicationContext());
            context = context;
        }
        return sInstance;
    }
    private DBHandler(Context c) throws SQLException {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        //this.context = c;
    }

    @Override
    public void onCreate(SQLiteDatabase db1) throws SQLException {
            // Create Imported Maps and User Settings Tables
            createMapsTable(db1);
            createSettingsTable(db1);
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
                 case 2:
                     // Version 3 new stuff
                     db1.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + KEY_SHOW_TRACKS + " TEXT");
                     db1.execSQL("UPDATE " + TABLE_SETTINGS + " SET " + KEY_SHOW_TRACKS + " = '0'");
             }

             //recreateSettingsTable(db1);
             //recreateMapsTable(db1);
        }
    }

    /*
    * CRUD OPERATIONS: Create, Read, Update, and Delete
    */
    // MAPS TABLE (details for each map)
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

    // Recreate database if they do not have all of the fields
    public void recreateMapsTable(SQLiteDatabase db1) throws SQLException {
        // Read what is currently in the database into mapList.
        // Delete database and recreate it. Restore data.
        ArrayList<PDFMap> mapList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MAPS;
        Cursor cursor = db1.rawQuery(selectQuery, null);

        // Save user maps in mapList
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
                if (cursor.getColumnCount() > 7) {
                    map.setFileSize(cursor.getString(7));
                    map.setDistToMap(cursor.getString(8));
                } else {
                    // Calculate the File Size
                    try{
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
                    map.setDistToMap("");
                }
                if (cursor.getColumnCount() == 10){
                    map.setMapOrientation(cursor.getString(9));
                } else {
                    map.setMapOrientation("none");
                }
                // Adding map to list
                mapList.add(map);
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }

        // remove database and add again
        deleteMapsTable(db1);

        // fill database
        for (int i=0; i<mapList.size(); i++){
            addMapToMapsTable(db1, mapList.get(i));
        }
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
        values.put(KEY_SHOW_TRACKS, "0");
        db1.insert(TABLE_SETTINGS, null, values);
    }
    /*public void recreateSettingsTable(SQLiteDatabase db1) throws SQLException{
        String selectQuery = "SELECT * FROM " + TABLE_SETTINGS;
        Cursor cursor = db1.rawQuery(selectQuery, null);
        // Set default values
        String map_sort = "date";
        String load_adj_maps = "1";
        String show_waypoints = "1";
        String show_all_waypoint_labels = "0";

        // Save old data
        if (cursor.moveToFirst()) {
            // id = cursor.getString(0); // should always be 1
            if (cursor.getColumnCount() > 1) map_sort = cursor.getString(1);
            if (cursor.getColumnCount() > 2) load_adj_maps = cursor.getString(2);
            if (cursor.getColumnCount() > 3) show_waypoints = cursor.getString(3);
            if (cursor.getColumnCount() > 4) show_all_waypoint_labels = cursor.getString(4);
        }
        if (cursor != null) {
            cursor.close();
        }
        // delete maps table
        db1.execSQL("DROP TABLE IF EXISTS "+ TABLE_SETTINGS);
        // Create settings table again
        createSettingsTable(db1);
        ContentValues values = new ContentValues();
        values.put(KEY_LOAD_ADJ_MAPS, load_adj_maps);
        values.put(KEY_MAP_SORT, map_sort);
        values.put(KEY_SHOW_WAYPOINTS, show_waypoints);
        values.put(KEY_SHOW_ALL_WAYPOINT_LABELS, show_all_waypoint_labels);
        String id = "1";
        db1.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
    }*/

    //-----------------
    // PUBLIC FUNCTIONS
    //------------------

    // Adding new PDF Map
    public Integer addMap(PDFMap map) throws SQLiteException {
        SQLiteDatabase db = this.getWritableDatabase();
        int index = addMapToMapsTable(db, map);
        return index;
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
                if (map.getFileSize() == "") {
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
                if (map.getDistToMap().equals("")) {
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

    //---------------------
    // Load Adjacent Maps?
    //---------------------
    public void setLoadAdjMaps(int load) throws SQLiteException{
        // Sets user preference, should load adjacent maps if current location goes off the map and onto another map?
        // Displays a drop down menu of maps to choose from. This is a checkbox on the maps more menu
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LOAD_ADJ_MAPS, Integer.toString(load));
        String id = "1";
        db.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
    }
    public int getLoadAdjMaps() throws SQLiteException {
        // Sets user preference, should load adjacent maps if current location goes off the map and onto another map?
        // Displays a drop down menu of maps to choose from. This is a checkbox on the maps more menu
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
}
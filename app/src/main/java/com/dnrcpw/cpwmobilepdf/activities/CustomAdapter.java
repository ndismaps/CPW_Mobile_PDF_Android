package com.dnrcpw.cpwmobilepdf.activities;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.dnrcpw.cpwmobilepdf.data.DBWayPtHandler;
import com.dnrcpw.cpwmobilepdf.model.PDFMap;
import com.dnrcpw.cpwmobilepdf.model.Tracks;
import com.dnrcpw.cpwmobilepdf.model.WayPts;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference; // to trap SQLException in dbExecutor.execute
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * Created by tammy on 9/7/2017.
 * List of Geo PDF Maps that have been scanned for geo bounds, margins, page size in pixels.
 * This info is stored in a database. The list is displayed in MainActivity.
 * On click displays a selected Geo PDF Map in PDFActivity.
 * Uses itextpdf and pdfium to read the PDF.
 */

public class CustomAdapter extends BaseAdapter {
    private final Context context;
    final private ExecutorService dbExecutor; // Pass this from the Activity
    ArrayList<PDFMap> pdfMaps;
    Double latNow, longNow;
    //private final DBHandler db;

    public CustomAdapter(Context c, ArrayList<PDFMap> pdfMaps, ExecutorService dbExecutor) {
        this.context = c;
        this.pdfMaps = pdfMaps;
        this.dbExecutor = dbExecutor;
    }

    public void SortByName(){
        // Sort array list pdfMaps of objects of type pdfMap alphabetically by name.
        Collections.sort((this.pdfMaps), PDFMap.NameComparator);
    }

    public void SortByNameReverse(){
        // Sort array list pdfMaps of objects of type pdfMap alphabetically by name.
        Collections.sort((this.pdfMaps), PDFMap.NameComparatorReverse);
    }

    public void SortByDate(){
        // Sort array list pdfMaps of objects of type pdfMap file modification date.
        Collections.sort((this.pdfMaps), PDFMap.DateComparator);
    }

    public void SortByDateReverse(){
        // Sort array list pdfMaps of objects of type pdfMap file modification date.
        Collections.sort((this.pdfMaps), PDFMap.DateComparatorReverse);
    }

    public void SortBySize(){
        // Sort array list pdfMaps of objects of type pdfMap file size.
        Collections.sort((this.pdfMaps), PDFMap.SizeComparator);
    }

    public void SortBySizeReverse(){
        // Sort array list pdfMaps of objects of type pdfMap file size.
        Collections.sort((this.pdfMaps), PDFMap.SizeComparatorReverse);
    }

    public void SortByProximity(){
        // Sort array list pdfMaps of objects of type pdfMap by proximity.
        Collections.sort((this.pdfMaps), PDFMap.ProximityComparator);
    }

    public void SortByProximityReverse(){
        // Sort array list pdfMaps of objects of type pdfMap by proximity.
        Collections.sort((this.pdfMaps), PDFMap.ProximityComparatorReverse);
    }

    public void setLocation(Location location){
        latNow = location.getLatitude();
        longNow = location.getLongitude();
    }

    public void setLocation(Double latitude, Double longitude){
        latNow = latitude;
        longNow = longitude;
    }

    public ArrayList<String> getAllMapNames() throws SQLException {
        // 4-16-26 for removing waypts that no longer have an existing map. Old bug.
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < this.pdfMaps.size(); i++) {
            if (!list.contains(this.pdfMaps.get(i).getName()))
                list.add(this.pdfMaps.get(i).getName());
        }
        return list;
    }

    public void removeWayPtsForOldMaps() {
        // 4-16-26 Make sure all waypoints have map files that exist. Old bug left waypoints when map was deleted!
        dbExecutor.execute(() -> {
            try {
                DBHandler db = DBHandler.getInstance(context);
                ArrayList<String> wayPtMapNames = db.getAllMapNames();
                ArrayList<String> existingMapNames = getAllMapNames();
                for (int i = 0; i < wayPtMapNames.size(); i++) {
                    if (!existingMapNames.contains(wayPtMapNames.get(i))) {
                        // map no longer exists. Remove waypoints for this map
                        db.deleteWayPts(wayPtMapNames.get(i));
                    }
                }
            } catch (SQLException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Database error: "+e.getMessage(),Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    public void checkIfExists() {
        // Check if the PDF exists in App directory. If not remove it from the database. Called by MainActivity.
        try {
            for (int i = 0; i < pdfMaps.size(); i++) {
                PDFMap map = pdfMaps.get(i);
                File file = new File(map.getPath());
                if (!file.exists()) {
                    //Log.d("CustomAdapter",context.getResources().getString(R.string.file) + map.getName() +  context.getResources().getString(R.string.noLongerExists));
                    File f = new File(map.getPath());
                    if (f.exists()) {
                        boolean result = f.delete();
                        if (!result){
                            Toast.makeText(context, context.getResources().getString(R.string.problemRemovingMap), Toast.LENGTH_LONG).show();
                        }
                    }
                    final int id = i;
                    dbExecutor.execute(() -> {
                        DBHandler db = DBHandler.getInstance(context);
                        db.deleteMap(map);
                        db.deleteWayPt(pdfMaps.get(id).getName());
                    });
                    pdfMaps.remove(i);
                    // delete thumbnail image also
                    if (map.getThumbnail() != null) {
                        File img = new File(map.getThumbnail());
                        if (img.exists()) {
                            boolean result2 = img.delete();
                            if (!result2)
                                Toast.makeText(context, context.getResources().getString(R.string.deleteThumbnail), Toast.LENGTH_LONG).show();
                        }
                    }
                    i--;
                }
            }
            notifyDataSetChanged();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(context,  context.getResources().getString(R.string.problemRemovingMap) + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (SQLException e){
            Toast.makeText(context,  context.getResources().getString(R.string.problemReadingDatabase) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void getDistToMap() {
        // calculate updated distances for all cells
        // Called from MainActivity onLocationResult when user location has changed by 1/10 of a mile
        // Or when latBefore is 0.0.
        try {
            if (latNow == null || longNow == null) {
                return;
            }
            for (int i = 0; i < pdfMaps.size(); i++) {
                PDFMap map = pdfMaps.get(i);

                String bounds = map.getBounds(); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
                if (bounds == null || bounds.length() == 0)
                    return; // it will be 0 length if is importing
                bounds = bounds.trim(); // remove leading and trailing spaces
           /* int pos = bounds.indexOf(" ");
            double lat1 = Double.parseDouble(bounds.substring(0, pos));
            bounds = bounds.substring(pos + 1); // strip off 'lat1 '
            pos = bounds.indexOf(" ");
            double long1 = Double.parseDouble(bounds.substring(0, pos));
            // FIND LAT2
            bounds = bounds.substring(pos + 1); // strip off 'long1 '
            pos = bounds.indexOf(" ");
            double lat2 = Double.parseDouble(bounds.substring(0, pos));
            // FIND LONG2
            pos = bounds.lastIndexOf(" ");
            double long2 = Double.parseDouble(bounds.substring(pos + 1));*/

                //String strBounds = bounds;
                // Get Latitude, Longitude bounds.
                // lat1 and long1 are the smallest values SW corner
                // lat2 and long2 are the largest NE corner
                String[] arrLatLong = bounds.split(" ");
                // convert strings to double
                Double[] LatLong = new Double[arrLatLong.length];
                for (int l = 0; l < arrLatLong.length; l++) {
                    LatLong[l] = Double.parseDouble(arrLatLong[l]);
                }
                // Find the smallest and largest values
                double lat1 = LatLong[0];
                double long1 = LatLong[1];
                double lat2 = LatLong[0];
                double long2 = LatLong[1];
                for (int l = 0; l < LatLong.length; l = l + 2) {
                    if (LatLong[l] < lat1) lat1 = LatLong[l];
                    if (LatLong[l] > lat2) lat2 = LatLong[l];
                    if (LatLong[l + 1] < long1) long1 = LatLong[l + 1];
                    if (LatLong[l + 1] > long2) long2 = LatLong[l + 1];
                }

                // Is on map?
                // On map
                if (latNow >= lat1 && latNow <= lat2 && longNow >= long1 && longNow <= long2) {
                    map.setMiles(0.0);
                    map.setDistToMap("onmap");
                    // Log.d(TAG, "updateDistToMap: " + map.getName() + " on map");
                }
                // Off map, calculate distance away
                else {
                    String direction = "";
                    double dist;
                    if (latNow < lat1) direction = "N";
                    else if (latNow > lat2) direction = "S";
                    if (longNow < long1) direction += "E";
                    else if (longNow > long2) direction += "W";

                    float[] results = new float[1];
                    switch (direction) {
                        case "S":
                            Location.distanceBetween(latNow, longNow, lat2, longNow, results);
                            break;
                        case "N":
                            Location.distanceBetween(latNow, longNow, lat1, longNow, results);
                            break;
                        case "E":
                            Location.distanceBetween(latNow, longNow, latNow, long1, results);
                            break;
                        case "W":
                            Location.distanceBetween(latNow, longNow, latNow, long2, results);
                            break;
                        case "SE":
                            Location.distanceBetween(latNow, longNow, lat2, long1, results);
                            break;
                        case "SW":
                            Location.distanceBetween(latNow, longNow, lat2, long2, results);
                            break;
                        case "NE":
                            Location.distanceBetween(latNow, longNow, lat1, long1, results);
                            break;
                        case "NW":
                            Location.distanceBetween(latNow, longNow, lat1, long2, results);
                            break;
                    }

                    String str = "    ";
                    String distStr;
                    dist = results[0] * 0.00062137119;
                    // use 2 decimal points
                    if (dist < 0.1){
                        map.setMiles(dist);
                        distStr = String.format(Locale.US, "%s %.2f %s %s", str, dist, context.getResources().getString(R.string.miles), direction);
                        map.setDistToMap(distStr);
                    }else {
                        // use 1 decimal point
                        map.setMiles(dist);
                        distStr = String.format(Locale.US, "%s %.1f %s %s", str, dist, context.getResources().getString(R.string.miles), direction);
                        map.setDistToMap(distStr);
                    }
                    //Log.d("CustomAdapter", "updateDistToMap: " + map.getName() + " " + map.getDistToMap());
                }
            }
        } catch (Exception e){
            Toast.makeText(context,  "Problem getting distance to map. " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getCount() {
        return pdfMaps.size();
    }

    @Override
    public Object getItem(int i) {
        return pdfMaps.get(i);
    }

    // return array of pdfMaps so that we do not need to get DBHandler or DBWayPtHander here 5-27-21
    //public ArrayList<PDFMap> getPdfMaps() {
    //    return pdfMaps;
    //}

    public void add(PDFMap pdfMap) {
        pdfMaps.add(pdfMap);
    }

    public void rename(int id, String name) {
        // Rename an imported map
        try {
            //DBHandler db = new DBHandler(c);
            PDFMap map;
            for (int i = 0; i < pdfMaps.size(); i++) {
                if (pdfMaps.get(i).getId() == id) {
                    map = pdfMaps.get(i);
                    String oldMapName = map.getName(); // save old name before we rename it!
                    try {
                        // Check if name has changed. If not return
                        //Log.d("CustomAdapter","Rename "+map.getName()+" to "+name);
                        if (name.equals(map.getName())) return;
                        File sdcard = context.getFilesDir();
                        File file = new File(map.getPath());
                        String fileName = name;
                        if (!name.endsWith(".pdf"))
                            fileName = name + ".pdf";
                        File newName = new File(sdcard, fileName);
                        boolean result = file.renameTo(newName);
                        if (!result)
                            Toast.makeText(context, "Can't rename file.", Toast.LENGTH_LONG).show();
                        map.setName(name);
                        map.setPath(sdcard + "/" + fileName);
                        dbExecutor.execute(() -> {
                            DBHandler db = DBHandler.getInstance(context);
                            // update waypoint map names
                            try{
                                db.updateMap(map); // update map table in database
                                WayPts wayPts = db.getWayPts(oldMapName);
                                for (int j = 0; j < wayPts.size(); j++) {
                                    wayPts.get(j).setName(name); // update class
                                    db.updateWayPt(wayPts.get(j)); // update waypts table in database
                                }
                                // Update track map names
                                Tracks tracks = db.getTracks(oldMapName);
                                for (int j = 0; j < tracks.size(); j++) {
                                    tracks.get(j).setMapName(name); // update tracks class
                                    db.updateTrack(tracks.get(j)); // update tracks table in database
                                }
                            } catch (Exception e) {
                                Toast.makeText(context, context.getResources().getString(R.string.problemReadingDatabase) + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(context, "Error renaming: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    notifyDataSetChanged();
                    break;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(context, "Problem renaming map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (SQLException e) {
            Toast.makeText(context, context.getResources().getString(R.string.problemReadingDatabase) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public long getItemId(int i) {
        // get the row associated with the item in the list
        return i;
    }

    public void removeAll() {
        // remove all maps from the list and the database
        try {
            //DBHandler db = new DBHandler(c);
            //DBWayPtHandler wpdb = new DBWayPtHandler(c);
            for (int i = pdfMaps.size()-1; i > -1;  i--) {
                PDFMap map = pdfMaps.get(i);
                Toast.makeText(context,"Deleting: "+map.getName(), Toast.LENGTH_LONG).show();
                File f = new File(map.getPath());
                if (f.exists()) {
                    boolean deleted = f.delete();
                    if (!deleted) {
                        Toast.makeText(context, context.getResources().getString(R.string.deleteFile), Toast.LENGTH_LONG).show();
                    }
                }
                dbExecutor.execute(() -> {
                    DBHandler db = DBHandler.getInstance(context);
                    db.deleteMap(map);
                    db.deleteTracks(map.getName()); // delete all tracks in this map
                    db.deleteWayPt(map.getName());// delete waypoints in this map
                });
                pdfMaps.remove(i);
                // delete thumbnail image also
                if (map.getThumbnail() != null) {
                    File img = new File(map.getThumbnail());
                    if (img.exists()) {
                        boolean deleted = img.delete();
                        if (!deleted) {
                            Toast.makeText(context, context.getResources().getString(R.string.deleteThumbnail), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
            notifyDataSetChanged();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(context, "Problem removing map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (SQLException e){
            Toast.makeText(context, context.getResources().getString(R.string.problemReadingDatabase) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void removeItem(int id) {
        // remove item i from the list and the database
        try {
            for (int i = 0; i < this.pdfMaps.size(); i++) {
                if (pdfMaps.get(i).getId() == id) {
                    PDFMap map = pdfMaps.get(i);
                    File f = new File(map.getPath());
                    if (f.exists()) {
                        boolean deleted = f.delete();
                        if (!deleted) {
                            Toast.makeText(context, context.getResources().getString(R.string.deleteFile), Toast.LENGTH_LONG).show();
                        }
                    }
                    //Toast.makeText(context,"Deleting: "+map.getName(), Toast.LENGTH_LONG).show();
                    dbExecutor.execute(() -> {
                        DBHandler db = DBHandler.getInstance(context);
                        db.deleteMap(map);
                        db.deleteWayPts(map.getName());
                        db.deleteTracks(map.getName());
                    });

                    pdfMaps.remove(i);
                    // delete thumbnail image also
                    String imgPath = map.getThumbnail();
                    if (imgPath != null) {
                        File img = new File(imgPath);
                        if (img.exists()) {
                            boolean deleted = img.delete();
                            if (!deleted) {
                                Toast.makeText(context, context.getResources().getString(R.string.deleteThumbnail), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    notifyDataSetChanged();
                    break;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(context, context.getResources().getString(R.string.problemRemovingMap) + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (SQLException e){
            Toast.makeText(context, context.getResources().getString(R.string.problemReadingDatabase) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    static class ViewHolder{
        TextView nameTxt;
        TextView fileSizeTxt;
        TextView distToMapTxt;
        ImageView locIcon;
        ImageView img;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            // INFLATE CUSTOM LAYOUT
            view = LayoutInflater.from(context).inflate(R.layout.model, viewGroup, false);
            holder.nameTxt = view.findViewById(R.id.nameTxt);
            holder.fileSizeTxt = view.findViewById(R.id.fileSizeTxt);
            holder.distToMapTxt = view.findViewById(R.id.distToMapTxt);
            holder.locIcon = view.findViewById(R.id.locationIcon);
            holder.locIcon.setVisibility(View.GONE);
            holder.img = view.findViewById(R.id.pdfImage);
            view.setBackgroundResource(android.R.color.transparent);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        view.setClickable(true);
        view.setLongClickable(true);
        final PDFMap pdfMap = (PDFMap) this.getItem(i);

        //  load thumbnail from file
        String path = pdfMap.getThumbnail();
        if (path == null) holder.img.setImageResource(R.drawable.pdf_icon);
        else {
            try {
                File imgFile = new File(pdfMap.getThumbnail());
                Bitmap myBitmap;
                myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (myBitmap != null)
                    holder.img.setImageBitmap(myBitmap);
                else
                    holder.img.setImageResource(R.drawable.pdf_icon);
            }catch (Exception ex){
                Toast.makeText(context, "Problem reading thumbnail.", Toast.LENGTH_LONG).show();
                holder.img.setImageResource(R.drawable.pdf_icon);
            }
        }

        // Failed to Load - remove it. GetMoreActivity calls PDFMap.importMap
        if (pdfMap.getName().equals(context.getResources().getString(R.string.loading))) {
            removeItem(pdfMap.getId());
        } else {
            holder.nameTxt.setText(pdfMap.getName());
            holder.fileSizeTxt.setText(pdfMap.getFileSize());
            // getDistToMap sets miles also
            String dist = pdfMap.getDistToMap();
            if (latNow != null) {
                if (!dist.equals("onmap")) {
                    holder.locIcon.setVisibility(View.GONE);
                    holder.distToMapTxt.setText(dist);
                }
                else {
                    holder.locIcon.setVisibility(View.VISIBLE);
                    holder.distToMapTxt.setText("");
                }
            } else
                holder.locIcon.setVisibility(View.GONE);
        }

        // VIEW ITEM CLICK
        view.setOnClickListener(view1 -> {
            // Display the map
            String bounds = pdfMap.getBounds();
            if (bounds == null) {
                Toast.makeText(context, "This file is not a Geo PDF. Missing GPTS Bounds.", Toast.LENGTH_LONG).show();
                return;
            }
            String mediaBox = pdfMap.getMediabox();
            if (mediaBox == null) {
                Toast.makeText(context, "This file is not a Geo PDF. Missing MediaBox.", Toast.LENGTH_LONG).show();
                return;
            }
            String viewPort = pdfMap.getViewport();
            if (viewPort == null) {
                Toast.makeText(context, "This file is not a Geo PDF. Missing Viewport.", Toast.LENGTH_LONG).show();
                return;
            }
            openPDFView(pdfMap.getPath(), pdfMap.getName(), bounds, mediaBox, viewPort);
        });


        // ITEM LONG CLICK - show activity to delete or rename item
        view.setOnLongClickListener(view12 -> {
            // Open edit activity
            Intent i1 = new Intent(context, EditMapNameActivity.class);
            i1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i1.putExtra("PATH", pdfMap.getPath());
            i1.putExtra("NAME", pdfMap.getName());
            i1.putExtra("BOUNDS", pdfMap.getBounds());
            i1.putExtra("ID", pdfMap.getId());
            i1.putExtra("IMG", pdfMap.getThumbnail());
            context.startActivity(i1);
            return false;
        });
        return view;
    }

    // OPEN PDF VIEW - load the map
    public void openPDFView(String path, String name, String bounds, String mediaBox, String viewPort) {
        Intent i = new Intent(context, PDFActivity.class);
        //i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("PATH", path);
        i.putExtra("NAME", name);
        i.putExtra("BOUNDS", bounds);
        i.putExtra("MEDIABOX", mediaBox);
        i.putExtra("VIEWPORT", viewPort);
        context.startActivity(i);
    }
}
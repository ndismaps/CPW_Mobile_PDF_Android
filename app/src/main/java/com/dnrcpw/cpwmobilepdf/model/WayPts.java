package com.dnrcpw.cpwmobilepdf.model;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;

public class WayPts {
    // Array of WayPt objects for a given map
    private final ArrayList<WayPt> wayPts;

    public WayPts(String mapName){
        this.wayPts = new ArrayList<>();
    }
    public WayPt add (String name, String desc, float x, float y, String colorName, String location){
        // Add a waypoint to the list.
        WayPt obj = new WayPt(name, desc, x, y, colorName, location);
        wayPts.add(obj);
        return obj;
    }

    public WayPt add (String name, String desc, float x, float y, String colorName, String timeStamp, String location){
        // Add a waypoint to the list.
        WayPt obj = new WayPt(name, desc, x, y, colorName, timeStamp, location);
        wayPts.add(obj);
        return obj;
    }

    public void add (int id, String name, String desc, float x, float y, String colorName, String time, String location){
        // Add a waypoint to the list.
        WayPt obj = new WayPt(id, name, desc, x, y, colorName, time, location);
        wayPts.add(obj);
    }

    public void remove(float x, float y){
        // Remove a waypoint from the list
        for (int i=0; i<wayPts.size(); i++){
            if ((wayPts.get(i).getX() == x) && (wayPts.get(i).getY() == y)) {
                wayPts.remove(i);
                break;
            }
        }
    }
    public void remove(String mapName){
        // Remove a waypoint from the list
        for (int i=0; i<wayPts.size(); i++){
            // used to be == mapName))
            if ((wayPts.get(i).getName().equals(mapName))) {
                wayPts.remove(i);
                break;
            }
        }
    }

    public void removeAll(){
        wayPts.clear();
    }

    public WayPt get(int i){
        return wayPts.get(i);
    }

    public int size(){
        return wayPts.size();
    }

    public void SortPts(){
        // Sort array list wayPts of objects of type wayPt by latitude.
        Collections.sort((this.wayPts), WayPt.LatComparator);
    }
}

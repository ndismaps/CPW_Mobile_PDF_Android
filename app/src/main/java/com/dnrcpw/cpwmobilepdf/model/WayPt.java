package com.dnrcpw.cpwmobilepdf.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

public class WayPt {
    // Holds one waypoint
    private int id;
    private String name; // pdf map name
    private String desc; // description
    private float x; // x screen coordinate
    private float y; // y screen coordinate
    private final String time; // date and time waypoint was created
    private String location; // lat, long
    private String colorName; //  color of pushpin image

    /*public WayPt(){}*/

    public WayPt(String name, String desc, float x, float y, String colorName, String location){
        this.name = name;
        this.desc = desc;
        this.x = x;
        this.y = y;
        Calendar cal = Calendar.getInstance();
        java.util.Date date = cal.getTime();
        DateFormat formattedDate = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US);
        this.time = formattedDate.format(date);
        this.colorName = colorName;
        this.location = location;
    }

    public WayPt(int id, String name, String desc, float x, float y, String colorName, String time, String location){
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.x = x;
        this.y = y;
        this.time = time;
        this.colorName = colorName;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public String getColorName() {
        return colorName;
    }

    public String getLocation() {
        return location;
    }
    public String getLat() {
        int pos = location.indexOf(",");
        return location.substring(0,pos);
    }
    public String getLong(){
        int pos = location.indexOf(",");
        return location.substring(pos+1);
    }

    public String getTime() {
        return time;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /*public void setTime(String time) {
        this.time = time;
    }*/

    // Sort arraylist of WayPts by lat (y) value more north points first
    public static Comparator<WayPt> LatComparator = (p1, p2) -> {
        float y1 = p1.getY();
        float y2 = p2.getY();
        // Return descending order
        if (y1 < y2) return 1;
        else if (y1 == y2) return 0;
        else return -1;
    };
}

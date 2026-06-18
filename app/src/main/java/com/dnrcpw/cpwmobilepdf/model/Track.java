package com.dnrcpw.cpwmobilepdf.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Track {
    // Array of Track objects for a given map
    private long id;
    public String mapName; // filename minus the path
    public String desc; // label or description
    public String colorName; // cyan, blue, red
    // track is an array list of each line segment (x1,y1,x2,y2) for one path
    private List<TrackSegment> trackSegments = new ArrayList<>();
    private String time;
    public Track(){}

    public Track(String mapName, String desc, String colorName, List<TrackSegment> trackSegments){
        this.mapName = mapName;
        this.desc = desc;
        this.colorName = colorName;
        this.trackSegments = trackSegments;
        Calendar cal = Calendar.getInstance();
        java.util.Date date = cal.getTime();
        DateFormat formattedDate = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US);
        this.time = formattedDate.format(date);
    }

    public Track(int id, String mapName, String desc, String trackSegments, String colorName, String time){
        // Called by Tracks/add and DBTrackHandler/getTracks to read the tracks from the database and fill the tracks ArrayList
        if (trackSegments.equals("")){
            this.trackSegments = null;
        }else {
            List<String> segments = Arrays.asList(trackSegments.split(","));
            double x1 = Double.parseDouble(segments.get(0));
            double y1 = Double.parseDouble(segments.get(1));
            for (int i = 2; i < segments.size(); i += 2) {
                double x2 = Double.parseDouble(segments.get(i));
                double y2 = Double.parseDouble(segments.get(i + 1));
                TrackSegment thisSegment = new TrackSegment(x1, y1, x2, y2);
                x1 = x2;
                y1 = y2;
                this.trackSegments.add(thisSegment);
            }
        }
        this.id = id;
        this.mapName = mapName;
        this.desc = desc;
        this.colorName = colorName;
        this.time = time;
    }
    public String getLineSegments(){
        // DBTrackHandler calls this to store it in the database as a string of comma-delimited lat, long points
        // return "x1,y1,x2,y2,x3,y3,..."
        // trackSegments contain x1,y1,x2,y2, where x2,y2 are the same as the previous x1,y1
        if (trackSegments == null || trackSegments.size() == 0) return "";
        String lineSegments = trackSegments.get(0).x1 + "," + trackSegments.get(0).y1;
        for (int i=1; i< trackSegments.size(); i++){
            if (lineSegments != "") lineSegments += ",";
            lineSegments += trackSegments.get(i).x2 + "," + trackSegments.get(i).y2;
        }
        return lineSegments;
    }
    public void addTrackSegment(double x1, double y1, double x2, double y2){
        TrackSegment trackSegment = new TrackSegment(x1,y1, x2, y2);
        this.trackSegments.add(trackSegment);
    }
    public List<TrackSegment> getTrackSegments(){
        return this.trackSegments;
    }
    public void setTrackSegments(List<TrackSegment> trackSegments){
        this.trackSegments = trackSegments;
    }

    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getMapName(){
        return this.mapName;
    }
    public void setMapName(String mapName){
        this.mapName = mapName;
    }
    public void setColorName(String colorName){
        this.colorName = colorName;
    }
    public String getColorName(){
        return this.colorName;
    }
    // Return description of the track
    public String getDesc(){
        return this.desc;
    }
    public void setDesc(String desc) {this.desc = desc;}
    public String getTime() {
        return time;
    }
}

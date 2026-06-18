package com.dnrcpw.cpwmobilepdf.model;

public class TrackSegment {
    // One line segment
    // x1, y1 one endpoint in long, lat
    // x2, y2 other endpoint in long, lat -105.0,40.34
    // Save the track so that it can redraw the user's path
    public double x1;
    public double x2;
    public double y1;
    public double y2;
    public TrackSegment(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /*public String getSegment(){
        // Called by DBTrackHandler to add one line segment to the track
        return this.x1 + "," + this.y1 + "," + this.x2 + "," + this.y2;
    }*/

    // getter routines uses for writing KMZ file
    public double getX1() {
        return x1;
    }

    public double getX2() {
        return x2;
    }

    public double getY1() {
        return y1;
    }

    public double getY2() {
        return y2;
    }

    // convert from lat, long to screen pixels
    public double getX1(double zoom, double marginx, double marginL, double long1, double longDiff, double pageWidth) {
        // return x1 in pixels at the zoom level
        return ((x1 - long1) / longDiff) * ((pageWidth * zoom) - marginx) + marginL;
    }
    public double getY1(double zoom, double marginy, double marginT, double lat2, double latDiff, double pageHeight){
        return ((lat2 - y1) / latDiff) * ((pageHeight * zoom) - marginy) + marginT;
    }
    public double getX2(double zoom, double marginx, double marginL, double long1, double longDiff, double pageWidth) {
        // return x1 in pixels at the zoom level
        return ((x2 - long1) / longDiff) * ((pageWidth * zoom) - marginx) + marginL;
    }
    public double getY2(double zoom, double marginy, double marginT, double lat2, double latDiff, double pageHeight){
        return ((lat2 - y2) / latDiff) * ((pageHeight * zoom) - marginy) + marginT;
    }
}

package com.dnrcpw.cpwmobilepdf.model;

public class TrackSegment {
    // One line segment
    // x1, y1 one endpoint in lat, long
    // x2, y2 other endpoint in lat long
    // Save the track so that it can redraw the user's path
    public float x1;
    public float x2;
    public float y1;
    public float y2;
    public float altitude1;
    public float altitude2;
    public TrackSegment(float x1, float y1, float altitude1, float x2, float y2, float altitude2) {
        this.x1 = x1;
        this.y1 = y1;
        this.altitude1 = altitude1;
        this.x2 = x2;
        this.y2 = y2;
        this.altitude2 = altitude2;
    }

    public String getSegment(){
        // Called by DBTrackHandler to add one line segment to the track
        return this.x1 + "," + this.y1 + "," + this.altitude1 + "," + this.x2 + "," + this.y2 + "," + this.altitude2;
    }

    // getter routines uses for writing KMZ file
    public float getX1() {
        return x1;
    }

    public float getX2() {
        return x2;
    }

    public float getY1() {
        return y1;
    }

    public float getY2() {
        return y2;
    }

    public float getAltitude1() {
        return altitude1;
    }

    public float getAltitude2() {
        return altitude2;
    }

    // convert from lat, long to screen pixels
    public float getX1(double zoom, double marginx, double marginL, double long1, double longDiff, double pageWidth) {
        // return x1 in pixels at the zoom level
        return (float) (((x1 - long1) / longDiff) * ((pageWidth * zoom) - marginx) + marginL);
    }
    public float getY1(double zoom, double marginy, double marginT, double lat2, double latDiff, double pageHeight){
        return (float) (((lat2 - y1) / latDiff) * ((pageHeight * zoom) - marginy) + marginT);
    }
    public float getX2(double zoom, double marginx, double marginL, double long1, double longDiff, double pageWidth) {
        // return x1 in pixels at the zoom level
        return (float) (((x2 - long1) / longDiff) * ((pageWidth * zoom) - marginx) + marginL);
    }
    public float getY2(double zoom, double marginy, double marginT, double lat2, double latDiff, double pageHeight){
        return (float) (((lat2 - y2) / latDiff) * ((pageHeight * zoom) - marginy) + marginT);
    }
}

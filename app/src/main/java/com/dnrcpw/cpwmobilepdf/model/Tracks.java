package com.dnrcpw.cpwmobilepdf.model;

import java.util.ArrayList;
import java.util.List;

public class Tracks {
    // Array of Track objects for a given map
    private final ArrayList<Track> tracks;

    public Tracks(){
        this.tracks = new ArrayList<>();
    }

    public Track add(String name, String desc, String colorName, List<TrackSegment> trackSegment, double minLong, double maxLong, double minLat, double maxLat){
        // Add a track to the list.
        if (trackSegment == null)
            trackSegment =  new ArrayList<>();
        Track obj = new Track(name, desc, colorName, trackSegment, minLong, maxLong, minLat, maxLat);
        tracks.add(obj); // add to ArrayList
        return obj;
    }

    public void add(int id, String name, String desc, String trackSegment, String colorName, String time, double minLong, double maxLong, double minLat, double maxLat){
        // Add a track to tracks array.
        // Called in DBHandler getTracks
        Track obj = new Track(id, name, desc, trackSegment, colorName, time, minLong, maxLong, minLat, maxLat);
        // Make sure this track has segments
        if (obj.getTrackSegments() != null)
            tracks.add(obj); // add to ArrayList
    }

    public void removeAllFromMap(String mapName){
        // Remove all tracks in a map
        for (int i=0; i<tracks.size(); i++){
            // used to be == mapName))
            if ((tracks.get(i).getMapName().equals(mapName))) {
                tracks.remove(i);
            }
        }
    }
    public void remove(Track track){
        // Remove a track from tracks
        long id = track.getId();
        for (int i=0; i<tracks.size(); i++){
            // used to be == mapName))
            if ((tracks.get(i).getId() == id)) {
                tracks.remove(i);
                break;
            }
        }
    }
    public void removeAll(){
        tracks.clear();
    }

    public Track get(int i){
        return tracks.get(i);
    }

    public int size(){
        return tracks.size();
    }
}


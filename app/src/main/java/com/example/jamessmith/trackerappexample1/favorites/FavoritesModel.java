package com.example.jamessmith.trackerappexample1.favorites;

/**
 * Created by James Smith on 13/02/2018.
 */

public class FavoritesModel {

    private String origin;
    private String destination;
    private String duration;
    private String distance;

    public FavoritesModel(String origin, String destination, String duration, String distance) {
        this.origin = origin;
        this.destination = destination;
        this.duration = duration;
        this.distance = distance;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getDuration() {
        return duration;
    }

    public String getDistance() {
        return distance;
    }
}

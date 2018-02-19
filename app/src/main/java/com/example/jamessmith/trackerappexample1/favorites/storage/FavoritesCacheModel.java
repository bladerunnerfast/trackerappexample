package com.example.jamessmith.trackerappexample1.favorites.storage;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by James Smith on 18/02/2018.
 */

public class FavoritesCacheModel implements Parcelable {

        private String origin;
        private String destination;
        private String distance;
        private String duration;
        private double originLatitude;
        private double originLongitude;
        private double destinationLatitude;
        private double destinationLongitude;

    public FavoritesCacheModel(String origin, String destination, String distance, String duration, double originLatitude,
                               double originLongitude, double destinationLatitude, double destinationLongitude) {
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.duration = duration;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
    }

    protected FavoritesCacheModel(Parcel in) {
        origin = in.readString();
        destination = in.readString();
        distance = in.readString();
        duration = in.readString();
        originLatitude = in.readDouble();
        originLongitude = in.readDouble();
        destinationLatitude = in.readDouble();
        destinationLongitude = in.readDouble();
    }

    public static final Creator<FavoritesCacheModel> CREATOR = new Creator<FavoritesCacheModel>() {
        @Override
        public FavoritesCacheModel createFromParcel(Parcel in) {
            return new FavoritesCacheModel(in);
        }

        @Override
        public FavoritesCacheModel[] newArray(int size) {
            return new FavoritesCacheModel[size];
        }
    };

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getDistance() {
        return distance;
    }

    public String getDuration() {
        return duration;
    }

    public double getOriginLatitude() {
        return originLatitude;
    }

    public double getOriginLongitude() {
        return originLongitude;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(origin);
        dest.writeString(destination);
        dest.writeString(distance);
        dest.writeString(duration);
        dest.writeDouble(originLatitude);
        dest.writeDouble(originLongitude);
        dest.writeDouble(destinationLatitude);
        dest.writeDouble(destinationLongitude);
    }
}

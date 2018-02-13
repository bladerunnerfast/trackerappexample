package com.example.jamessmith.trackerappexample1.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.jamessmith.trackerappexample1.backend.connectionmanagement.ManageConnection;
import com.example.jamessmith.trackerappexample1.backend.model.GoogleDataModel;
import com.example.jamessmith.trackerappexample1.backend.model.Leg;
import com.example.jamessmith.trackerappexample1.backend.model.Step;
import com.example.jamessmith.trackerappexample1.backend.observable.CustomObservable;
import com.example.jamessmith.trackerappexample1.favorites.FavoritesModel;
import com.example.jamessmith.trackerappexample1.filemanagement.FileManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by jamessmith on 07/02/2018.
 */

public class TrackerService extends Service  implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final IBinder binder = new LocalBinder();
    private MarkerOptions markerOptions = new MarkerOptions();
    private static GoogleMap googleMap;
    private Geocoder geocoder;
    private static GoogleDataModel mModel;
    private boolean isTrackingEnabled = true;
    private final Intent intent = new Intent("updateMapActivity");
    private List<Leg> legs;
    private static String googleApiKey = "AIzaSyA6UeXLie3DBLBNRU0YT4HCOZmrou8-Os8";

    private static final String TAG = TrackerService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public void setRoute(double oLatitude, double oLongitude, double dLatitude, double dLongitude) {
        downloadData(googleApiKey, oLatitude, oLongitude, dLatitude, dLongitude, "enabled", "walking", "enabled");
    }

    public void setRoute(String origin, String destination){

        if((origin != null) && (origin.length() > 0) && (destination != null) && (destination.length() > 0)) {

            double oLat = getLoc(origin, 1);
            double oLon = getLoc(origin, 2);
            double dLat = getLoc(destination, 1);
            double dLon = getLoc(destination, 2);

            downloadData(googleApiKey, oLat, oLon, dLat, dLon, "enabled","walking","enabled");
        }
    }

    private double getLoc(String address, int sel){

        geocoder = new Geocoder(this);

        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);

            if(sel == 1) {
                return addresses.get(0).getLatitude();
            }

            else if(sel == 2) {
                return addresses.get(0).getLongitude();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -99999999.999999999;
        }
        return -99999999.999999999;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        intent.putExtra("instruction", "updateCurrentLocation");
        intent.putExtra("lastLat", location.getLatitude());
        intent.putExtra("latLon", location.getLongitude());
        sendBroadcast(intent);
    }

    private void drawMarkers(double sLat, double sLon, double eLat, double eLon) {
        //Place current location marker
        LatLng startLatLng = new LatLng(sLat, sLon);

        googleMap.clear();
        markerOptions.position(startLatLng);
        markerOptions.title("Start Here!!!");
        markerOptions.snippet(getGeoCodedAddress(sLat, sLon));
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        googleMap.addMarker(markerOptions);

        LatLng endPointLaLng = new LatLng(eLat, eLon);
        markerOptions.position(endPointLaLng);
        markerOptions.title("Finish");
        markerOptions.snippet("Duration " + getDuration());

        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        googleMap.addMarker(markerOptions);
        setCenterCoordinates(sLat, sLon, eLat, eLon);
    }

    public void setCenterCoordinates(double startLat, double startLon, double endLat, double endLon) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(startLat, startLon));
        builder.include(new LatLng(endLat, endLon));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 3));
    }

    @NonNull
    public String getGeoCodedAddress(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this);

        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

            if((addresses != null) && (addresses.size() > 0)) {
                return addresses.get(0).getAddressLine(0) + "\n" + addresses.get(0).getPostalCode();
            }else{
                return "Unavailable.";
            }
        } catch (IOException e) {
            Log.v(TAG, e.getMessage());
            return "Unavailable.";
        }
    }

    @Nullable
    public PolylineOptions getPolylines() {

        if((mModel != null) && (mModel.getRoutes().size() > 0)) {
            if(googleMap != null) {
                Step step;
                String polyline;
                ArrayList<LatLng> routelist = new ArrayList<>();
                ArrayList<LatLng> decodelist;
                PolylineOptions polylineOptions = new PolylineOptions().width(10).color(Color.RED);

                for (int leg = 0; leg < legs.size(); leg ++) {

                    List<Step> steps = mModel.getRoutes().get(0).getLegs().get(leg).getSteps();

                    for (int i = 0; i < steps.size(); i++) {
                        step = steps.get(i);
                        routelist.add(new LatLng(step.getStartLocation().getLat(), step.getStartLocation().getLng()));
                        polyline = step.getPolyline().getPoints();
                        decodelist = decodePoly(polyline);
                        routelist.addAll(decodelist);
                        routelist.add(new LatLng(step.getEndLocation().getLat(), step.getEndLocation().getLng()));
                    }

                    if (routelist.size() > 0) {
                        for (int i = 0; i < routelist.size(); i++) {
                            polylineOptions.add(routelist.get(i));
                        }

                        return polylineOptions;
                    }
                }
            }
        }

        return null;
    }

    public static ArrayList<LatLng> decodePoly(String encoded) {
        ArrayList<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;

            do {
                b = encoded.charAt(index ++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index ++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng position = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(position);
        }
        return poly;
    }

    private void downloadData(String apiKey, final double oLat, final double oLon, final double dLat, final double dLon, String sensor, String mode, String traffic) {

        final ManageConnection restfulClient = new ManageConnection();
        CustomObservable api = restfulClient.getRest().build().create(CustomObservable.class);

        String start = oLat + "," + oLon;
        String end = dLat + "," + dLon;

        restfulClient.getCompositeSubscription().add(api.getMapData(apiKey, start, end, sensor, mode, traffic)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<GoogleDataModel>() {

                    @Override
                    public void onCompleted() {
                        legs = mModel.getRoutes().get(0).getLegs();

                        drawMarkers(oLat, oLon, dLat, dLon);
                        storeTrackedData();

                        intent.putExtra("instruction", "loadRoutes");
                        intent.putExtra("status", "successful");
                        sendBroadcast(intent);

                        restfulClient.getCompositeSubscription().unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e != null) {
                            Log.v(TAG, e.toString());
                            Toast.makeText(getApplicationContext(), "Failed to download data", Toast.LENGTH_SHORT).show();
                            restfulClient.getCompositeSubscription().unsubscribe();
                        }
                    }

                    @Override
                    public void onNext(GoogleDataModel model) {
                        mModel = model;
                    }
                })
        );
    }

    public void setGoogleMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public MarkerOptions getMarkerOptions() {
        return markerOptions;
    }

    private void storeTrackedData(){

        if(isTrackingEnabled) {

            new AsyncTask<Void, Void, Void>() {

                FavoritesModel favoritesModel;
                FileManager fileManager;
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    fileManager = new FileManager(getApplicationContext());
                }

                @Override
                protected Void doInBackground(Void... voids) {

                    for(int i = 0; i < legs.size(); i ++){
                        favoritesModel = new FavoritesModel(
                                legs.get(i).getStartAddress(),
                                legs.get(i).getEndAddress(),
                                legs.get(i).getDuration().getText(),
                                legs.get(i).getDistance().getText());
                        fileManager.setFavoritesList(favoritesModel);
                    }

                    return null;
                }
            }.execute();
        }
    }

    public String getDuration() {

        String duration;
        duration = legs.get(0).getDuration().getText();
        return duration;
    }

    public class LocalBinder extends Binder {
        public TrackerService getService() {
            return TrackerService.this;
        }
    }
}

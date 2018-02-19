package com.example.jamessmith.trackerappexample1.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.example.jamessmith.trackerappexample1.backend.connectionmanagement.ManageConnection;
import com.example.jamessmith.trackerappexample1.backend.model.GoogleDataModel;
import com.example.jamessmith.trackerappexample1.backend.model.Leg;
import com.example.jamessmith.trackerappexample1.backend.model.Step;
import com.example.jamessmith.trackerappexample1.backend.observable.CustomObservable;
import com.example.jamessmith.trackerappexample1.favorites.storage.FavoritesCache;
import com.example.jamessmith.trackerappexample1.favorites.storage.FavoritesCacheModel;
import com.example.jamessmith.trackerappexample1.ultilities.PolyDecoder;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by james smith on 07/02/2018.
 */

public class TrackerService extends Service {

    private boolean isServiceRunning = false;
    private final IBinder binder = new LocalBinder();
    private MarkerOptions markerOptions = new MarkerOptions();
    private static GoogleMap googleMap;
    private static GoogleDataModel mModel;
    private boolean isTrackingEnabled = true;
    private final Intent intent = new Intent("updateMapFragment");
    private List<Leg> legs;
    private List<LatLng> usersPolyLine;
    private Handler handler = new Handler();
    private static final String googleApiKey = "AIzaSyA6UeXLie3DBLBNRU0YT4HCOZmrou8-Os8";
    private static final String sensor = "enabled", mode = "walking", traffic = "enabled";

    private FavoritesCache favoritesCache;

    private static final String TAG = TrackerService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        isServiceRunning = true;
        favoritesCache = new FavoritesCache(getApplicationContext());
        usersPolyLine = new ArrayList<>();

        runnable.run();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setRoute(double oLatitude, double oLongitude, double dLatitude, double dLongitude) {
        downloadData(googleApiKey, oLatitude, oLongitude, dLatitude, dLongitude);
    }

    public void setRoute(String origin, String destination) {

        if ((origin != null) && (origin.length() > 0) && (destination != null) && (destination.length() > 0)) {

            double oLat = getLoc(origin, 1);
            double oLon = getLoc(origin, 2);
            double dLat = getLoc(destination, 1);
            double dLon = getLoc(destination, 2);

            downloadData(googleApiKey, oLat, oLon, dLat, dLon);
        }
    }

    private double getLoc(String address, int sel) {

        Geocoder geocoder = new Geocoder(this);

        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);

            if ((addresses != null) && (addresses.size() > 0)) {
                if (sel == 1) {
                    return addresses.get(0).getLatitude();
                } else if (sel == 2) {
                    return addresses.get(0).getLongitude();
                }
            }

        } catch (IOException e) {
            Log.v(TAG, e.getMessage());
            return 0;
        }
        return 0;
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            initLocationUpdates();
        }
    };

    private void initLocationUpdates() {
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    usersPolyLine.add(new LatLng(location.getLatitude(), location.getLongitude()));
                    intent.putExtra("instruction", "setPolyLines");
                    sendBroadcast(intent);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, e.getLocalizedMessage());
                    }
                });

        if(isTrackingEnabled) {
            handler.postDelayed(runnable, 500);
        }
    }

    @Nullable
    public PolylineOptions getUsersPosition() {

        PolylineOptions polylineOptions = new PolylineOptions().width(5).color(Color.BLUE);

        if((usersPolyLine != null) && (usersPolyLine.size() > 0)) {
            for(int i = 0; i < usersPolyLine.size(); i ++) {
                polylineOptions.add(usersPolyLine.get(i));
            }
        }

        else {
            return null;
        }

        return polylineOptions;
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
    public PolylineOptions getPolyLines() {

        if((mModel != null) && (mModel.getRoutes().size() > 0)) {
            if(googleMap != null) {
                Step step;
                String polyline;
                ArrayList<LatLng> routeList = new ArrayList<>();
                ArrayList<LatLng> decodeList;
                PolylineOptions polylineOptions = new PolylineOptions().width(5).color(Color.RED);

                for (int leg = 0; leg < legs.size(); leg ++) {

                    List<Step> steps = mModel.getRoutes().get(0).getLegs().get(leg).getSteps();

                    for (int i = 0; i < steps.size(); i++) {
                        step = steps.get(i);
                        routeList.add(new LatLng(step.getStartLocation().getLat(), step.getStartLocation().getLng()));
                        polyline = step.getPolyline().getPoints();
                        decodeList = PolyDecoder.decodePoly(polyline);
                        routeList.addAll(decodeList);
                        routeList.add(new LatLng(step.getEndLocation().getLat(), step.getEndLocation().getLng()));
                    }

                    if (routeList.size() > 0) {
                        for (int i = 0; i < routeList.size(); i++) {
                            polylineOptions.add(routeList.get(i));
                        }

                        return polylineOptions;
                    }
                }
            }
        }

        return null;
    }

    private void downloadData(String apiKey, final double oLat, final double oLon, final double dLat, final double dLon) {

        final ManageConnection restfulClient = new ManageConnection();
        CustomObservable api = restfulClient.getRest().build().create(CustomObservable.class);

        String start = oLat + "," + oLon;
        String end = dLat + "," + dLon;

        restfulClient.getCompositeSubscription().add(api.getMapData(apiKey, start, end, sensor, mode, traffic)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<GoogleDataModel>() {

                    @Override
                    public void onNext(GoogleDataModel model) {
                        mModel = model;
                    }

                    @Override
                    public void onCompleted() {
                        legs = mModel.getRoutes().get(0).getLegs();

                        drawMarkers(oLat, oLon, dLat, dLon);

                        if(favoritesCache != null) {
                            new StoreData(legs, favoritesCache).execute();
                        }

                        intent.putExtra("instruction", "loadRoutes");
                        intent.putExtra("status", "successful");
                        sendBroadcast(intent);

                        restfulClient.getCompositeSubscription().unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e != null) {
                            Log.v(TAG, e.toString());
                            restfulClient.getCompositeSubscription().unsubscribe();
                        }
                    }
                }
                )
        );
    }

    public void setGoogleMap(GoogleMap googleMap) {
        TrackerService.googleMap = googleMap;
    }

    public MarkerOptions getMarkerOptions() {
        return markerOptions;
    }

    public String getDuration() {

        String duration;
        duration = legs.get(0).getDuration().getText();
        return duration;
    }

    public boolean isServiceRunning() {
        return isServiceRunning;
    }

    public void setTrackingEnabled(boolean trackingEnabled) {
        isTrackingEnabled = trackingEnabled;
    }

    public class LocalBinder extends Binder {
        public TrackerService getService() {
            return TrackerService.this;
        }
    }

    public static class StoreData extends AsyncTask<Void, Void, Void> {

        private List<Leg> legs;
        private FavoritesCacheModel favoritesCacheModel;
        private FavoritesCache favoritesCache;

        public StoreData(List<Leg> legs, FavoritesCache favoritesCacheDB) {
            this.legs = legs;
            this.favoritesCache = favoritesCacheDB;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            for (int i = 0; i < legs.size(); i++) {
                favoritesCacheModel = new FavoritesCacheModel(
                        legs.get(i).getStartAddress(),
                        legs.get(i).getEndAddress(),
                        legs.get(i).getDistance().getText(),
                        legs.get(i).getDuration().getText(),
                        legs.get(i).getStartLocation().getLat(),
                        legs.get(i).getStartLocation().getLng(),
                        legs.get(i).getEndLocation().getLat(),
                        legs.get(i).getEndLocation().getLng());

                if(!favoritesCache.setFavorites(favoritesCacheModel)) {
                    Log.v(TAG, "Failed to add entry.");
                }
            }

            return null;
        }
    }
}

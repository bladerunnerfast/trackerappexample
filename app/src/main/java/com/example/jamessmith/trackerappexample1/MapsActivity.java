package com.example.jamessmith.trackerappexample1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.jamessmith.trackerappexample1.favorites.FavoritesAdapter;
import com.example.jamessmith.trackerappexample1.favorites.FavoritesModel;
import com.example.jamessmith.trackerappexample1.filemanagement.FileManager;
import com.example.jamessmith.trackerappexample1.service.TrackerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    @BindView(R.id.rl_favorites_bottomSheetLayout) RelativeLayout bottomSheet;
    @BindView(R.id.rv_favorites_recycler_view) RecyclerView _favoritesList;
    private static final String TAG = MapsActivity.class.getSimpleName();

    private BottomSheetBehavior bottomSheetBehavior;

    private MarkerOptions markerOptions = new MarkerOptions();
    private GoogleApiClient mGoogleApiClient;
    private Marker currentLocationMarker;
    private LocationRequest mLocationRequest;
    private LocationManager manager;
    private Intent intent;
    private static TrackerService trackerService;
    private GoogleMap googleMap;
    private IntentFilter intentFilter;
    private BroadcastReceiver broadcastReceiver;
    private SupportMapFragment mapFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        mapFrag = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(!checkLocationPermission()) {
            Toast.makeText(this, "Features require access to function.", Toast.LENGTH_LONG).show();
        }

        if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }

        intent = new Intent(this, TrackerService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onLocationChanged(Location location) {}

    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("GPS does not appear to be enabled")
                .setCancelable(false)
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    private void initBottomSheet(){

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {

                CoordinatorLayout.LayoutParams params;
                float dpRatio = getApplicationContext().getResources().getDisplayMetrics().density;

                switch (newState) {

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        bottomSheet.setVisibility(View.INVISIBLE);
                        break;

                    case BottomSheetBehavior.STATE_EXPANDED:
                        _favoritesList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        _favoritesList.setItemAnimator(new DefaultItemAnimator());
                        bottomSheet.setVisibility(View.VISIBLE);

                        initAdapter();
                        break;

                    case BottomSheetBehavior.STATE_SETTLING:
                        if ((bottomSheetBehavior.getState() <= BottomSheetBehavior.STATE_COLLAPSED) && (bottomSheetBehavior.getState() > BottomSheetBehavior.STATE_EXPANDED)){
                            params = new CoordinatorLayout.LayoutParams(100 * Math.round(dpRatio), 50);
                            params.setAnchorId(R.id.rl_favorites_bottomSheetLayout);
                        }
                        break;
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                bottomSheet.setAlpha(slideOffset);
            }
        });

        bottomSheet.setVisibility(View.INVISIBLE);
    }

    private void initAdapter(){

        FileManager fileManager = new FileManager(getApplicationContext());
        List<FavoritesModel> favoritesList = fileManager.getFavoritesList();

        if((favoritesList != null) && (favoritesList.size() > 0)) {
            FavoritesAdapter favoritesAdapter = new FavoritesAdapter(this, favoritesList);
            _favoritesList.setAdapter(favoritesAdapter);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            TrackerService.LocalBinder binder = (TrackerService.LocalBinder) service;
            trackerService = binder.getService();
            Log.v(TAG, "Connected to Service");

            initBroadcastReciever();
            setupMap();
            initBottomSheet();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            trackerService = null;
            Log.v(TAG, "Disconnected from Service");
        }
    };

    private void setupMap() {

      OnMapReadyCallback onMapReadyCallback =  new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMapData) {
                googleMap = googleMapData;

                try{
                    googleMap.setMyLocationEnabled(true);
                    trackerService.setGoogleMap(googleMap);

                    googleMapData.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(final LatLng latLng) {

                            LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            trackerService.setRoute(location.getLatitude(), location.getLongitude(),
                                    latLng.latitude, latLng.longitude);

                        }
                    });

                }catch (SecurityException e) {
                    Log.v(TAG, e.getMessage());
                }
            }
        };

      mapFrag.getMapAsync(onMapReadyCallback);
    }

    private void initBroadcastReciever() {

        intentFilter = new IntentFilter("updateMapActivity");

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent != null) {
                    if("loadRoutes".equals(intent.getStringExtra("instruction"))) {
                        if("successful".equals(intent.getStringExtra("status"))) {
                            Log.v(TAG, "status");
                            if(googleMap != null) {
                                if(trackerService.getPolylines() != null) {
                                    googleMap.addPolyline(trackerService.getPolylines());
                                    markerOptions.draggable(true);
                                    markerOptions = trackerService.getMarkerOptions();
                                    googleMap.addMarker(markerOptions);

                                } else {
                                    Log.v(TAG, "Error while retrieving route data");
                                }
                            }
                        }

                        else if("updateCurrentLocation".equals(intent.getStringExtra("instruction"))) {

                            if (currentLocationMarker != null) {
                                        currentLocationMarker.remove();
                            }

                            double lastLat = intent.getDoubleExtra("lastLat", 0);
                            double latLon = intent.getDoubleExtra("lastLon", 0);

                            Toast.makeText(getApplicationContext(), "lat: " + lastLat, Toast.LENGTH_SHORT).show();
                            //Place current location marker
                            LatLng latLng = new LatLng(lastLat, latLon);
                            markerOptions.position(latLng);
                            markerOptions.title("Your here");
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                            currentLocationMarker = googleMap.addMarker(markerOptions);
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(4));
                        }
                    }

                    else if("updateFromList".equals(intent.getStringExtra("instruction"))) {
                        String origin = intent.getStringExtra("origin");
                        String destination = intent.getStringExtra("destination");
                        trackerService.setRoute(origin, destination);
                    }
                }
            }
        };

        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(broadcastReceiver);
        }catch (Exception e){/* Left black*/ }
    }
}
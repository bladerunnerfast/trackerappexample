package com.example.jamessmith.trackerappexample1;

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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.jamessmith.trackerappexample1.service.TrackerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    @BindView(R.id.map) MapView mapFrag;

    private static final String TAG = MapsFragment.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private MarkerOptions markerOptions = new MarkerOptions();
    private GoogleApiClient mGoogleApiClient;
    private Marker currentLocationMarker;
    private static TrackerService trackerService;
    private GoogleMap googleMap;
    private BroadcastReceiver broadcastReceiver;
    private Intent serviceIntent;
    private boolean isBinding = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_maps, container, false);
        ButterKnife.bind(this, view);

        mapFrag.onCreate(savedInstanceState);
        mapFrag.onResume();

        try {
            MapsInitializer.initialize(getContext());
        } catch (Exception e) {
            Log.v(TAG, e.getMessage());
        }

        LocationManager manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);


        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        setBindConnection();
    }

    private void setBindConnection() {

        if(!isBinding) {
            serviceIntent = new Intent(getContext(), TrackerService.class);
            getContext().bindService(serviceIntent, serviceConnection, getContext().BIND_AUTO_CREATE);
            getContext().startService(serviceIntent);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                    }

                } else {Toast.makeText(getContext(), "permission denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Connection has been suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v(TAG, connectionResult.getErrorMessage());
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            TrackerService.LocalBinder binder = (TrackerService.LocalBinder) service;
            trackerService = binder.getService();
            isBinding = true;
            Log.v(TAG, "Connected to Service");

            initBroadcastReceiver();
            setupMap();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            trackerService = null;
            isBinding = false;
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

                            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
                            mFusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location currentLocation) {

                                            if (currentLocation != null) {
                                                trackerService.setRoute(currentLocation.getLatitude(), currentLocation.getLongitude(), latLng.latitude, latLng.longitude);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, e.getLocalizedMessage());
                                        }
                                    });
                        }
                    });

                }catch (SecurityException e) {
                    Log.v(TAG, e.getMessage());
                }
            }
        };

      mapFrag.getMapAsync(onMapReadyCallback);
    }

    private void initBroadcastReceiver() {

        IntentFilter intentFilter = new IntentFilter("updateMapFragment");

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent != null) {
                    if("loadRoutes".equals(intent.getStringExtra("instruction"))) {
                        if ("successful".equals(intent.getStringExtra("status"))) {
                            if (googleMap != null) {
                                if (trackerService.getPolyLines() != null) {
                                    googleMap.addPolyline(trackerService.getPolyLines());
                                    markerOptions.draggable(true);
                                    markerOptions = trackerService.getMarkerOptions();
                                    googleMap.addMarker(markerOptions);

                                } else {
                                    Log.v(TAG, "Error while retrieving route data");
                                }
                            }
                        }
                    }

                    else if("setPolyLines".equals(intent.getStringExtra("instruction"))) {

                        if(trackerService.getUsersPosition() != null) {
                            googleMap.addPolyline(trackerService.getUsersPosition());
                        }
                    }

                    else if("updateFromList".equals(intent.getStringExtra("instruction"))) {
                        String origin = intent.getStringExtra("origin");
                        String destination = intent.getStringExtra("destination");
                        String oReplacement = origin.replace("Origin:", "");
                        String dReplacement = destination.replace("Destination:", "");
                        Toast.makeText(getContext(), "Origin: " + oReplacement, Toast.LENGTH_SHORT);
                        trackerService.setRoute(oReplacement, dReplacement);
                    }

                    else if("selectedOptions".equals(intent.getStringExtra("instruction"))) {
                        int mapMode = intent.getIntExtra("mapMode", -1);

                        if((mapMode > -1) && (mapMode <= 3 )) {
                            setMapType(mapMode);
                        }
                    }
                }
            }
        };

        getContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setMapType(int index) {

        switch (index) {
            case 0:
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case 1:
                googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case 2:
                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case 3:
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            default:
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        setBindConnection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {

            getContext().unregisterReceiver(broadcastReceiver);

            if((trackerService != null) && (trackerService.isServiceRunning())) {
                trackerService.stopService(serviceIntent);
            }

        }catch (Exception e){/* Left black*/ }
    }
}
package com.example.jamessmith.trackerappexample1;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.jamessmith.trackerappexample1.favorites.FavoritesAdapter;
import com.example.jamessmith.trackerappexample1.favorites.FavoritesModel;
import com.example.jamessmith.trackerappexample1.filemanagement.FileManager;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.rl_favorities_bottom_sheet_layout) RelativeLayout _bottomSheet;
    @BindView(R.id.rv_favorites_recycler_view) RecyclerView _recyclerView;
    @BindView(R.id.fab_favorites_btn) FloatingActionButton _favoritesBtn;
    @BindView(R.id.nav_view) NavigationView navigationView;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private Intent intent;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        initBottomSheet();

        if(_recyclerView != null){
            _recyclerView.setLayoutManager(new LinearLayoutManager(this));
            _recyclerView.setItemAnimator(new DefaultItemAnimator());
            initAdapter();
        }

        if(isStoragePermissionGranted()) {

        } else {
            Toast.makeText(this, "Storage permission is required.", Toast.LENGTH_SHORT).show();
        }

        if(checkLocationPermission()) {
            Fragment fragment = new MapsFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment, null).commit();
        } else {
            Toast.makeText(this, "Features will not funcation without access to location hardware.", Toast.LENGTH_LONG).show();
        }

        _favoritesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                accessList();
                // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        intent = new Intent("updateMapFragment");
        intent.putExtra("instruction", "selectedOptions");

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_normal:
                intent.putExtra("mapMode", 0);
                break;
            case R.id.nav_satellite:
                intent.putExtra("mapMode", 1);
                break;
            case R.id.nav_terrain:
                intent.putExtra("mapMode", 2);
                break;
            case R.id.nav_hybrid:
                intent.putExtra("mapMode", 3);
                break;
            case R.id.nav_cycle:
                intent.putExtra("satOption", "cycleModeSelected");
                break;
            case R.id.nav_walking:
                intent.putExtra("satOption", "walkingModeSelected");
                break;
            case R.id.nav_driving:
                intent.putExtra("satOption", "drivingModeSelected");
                break;
            case R.id.nav_tracking:
                intent.putExtra("settings", "trackingModeStatus");
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        sendBroadcast(intent);
        return true;
    }


    private void accessList(){

        _bottomSheet.setVisibility(View.VISIBLE);
        if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }else if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED){
            if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN){
                initBottomSheet();
            }
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void initBottomSheet() {

        bottomSheetBehavior = BottomSheetBehavior.from(_bottomSheet);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {

                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        bottomSheet.setVisibility(View.INVISIBLE);
                        break;

                    case BottomSheetBehavior.STATE_EXPANDED:
                        if(_recyclerView != null){
                            _recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            _recyclerView.setItemAnimator(new DefaultItemAnimator());
                            bottomSheet.setVisibility(View.VISIBLE);

                            initAdapter();
                        }
                        break;
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                bottomSheet.setAlpha(slideOffset);
            }
        });

        _bottomSheet.setVisibility(View.INVISIBLE);
    }


    private void initAdapter(){

        FileManager fileManager = new FileManager(this);
        List<FavoritesModel> favoritesList = fileManager.getFavoritesList();

        if((favoritesList != null) && (favoritesList.size() > 0)) {
            FavoritesAdapter favoritesAdapter = new FavoritesAdapter(this, favoritesList);
            _recyclerView.setAdapter(favoritesAdapter);
        }
    }
    public boolean checkLocationPermission() {

        if(Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission has been granted");
                return true;
            } else {
                Log.v(TAG,"Permission has been revoked");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //Automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission has been granted");
            return true;
        }
    }
}

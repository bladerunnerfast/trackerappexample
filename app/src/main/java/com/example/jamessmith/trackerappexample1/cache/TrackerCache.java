package com.example.jamessmith.trackerappexample1.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jamessmith on 07/02/2018.
 */

public class TrackerCache extends SQLiteOpenHelper {


    private static final String databaseName = "Tracker";
    private static final String tableName = "trackedData";
    private static final int version = 1;

    private static final String id = "id";
    private static final String startLatitude = "startLatitude";
    private static final String startLongitude = "startLongitude";
    private static final String endLatitude = "endLatitude";
    private static final String endLongitude = "endLongitude";
    private static final String legCount = "legCount";
    private static final String currentLegLocation = "currentLegLocation";
    private static final String address = "address";

    private static final String tableStructure = "CREATE TABLE " + tableName + "( " + id + " INTEGER PRIMARY KEY AUTOINCREMENT, " + startLatitude + "double," +
            startLongitude + " DOUBLE, " + endLatitude + " DOUBLE," + endLongitude + " DOUBLE, " + legCount + " INTEGER, " + currentLegLocation + " TEXT, " + address + " TEXT)";

    public TrackerCache(Context context) {
        super(context, databaseName, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(tableStructure);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        onCreate(db);
    }
}

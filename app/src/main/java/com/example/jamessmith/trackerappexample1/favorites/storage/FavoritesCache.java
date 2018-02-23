package com.example.jamessmith.trackerappexample1.favorites.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Smith on 18/02/2018.
 */

public class FavoritesCache extends SQLiteOpenHelper{

    private static final String DATABASE = "favoritesDatabase";
    private static final int VERSION = 1;
    private static final String TABLE = "favoritesTable";

    private static final String ID = "id";
    private static final String ORIGIN = "origin";
    private static final String DESTINATION = "destination";
    private static final String DISTANCE = "distance";
    private static final String DURATION = "duration";
    private static final String oLAT = "oLatitude";
    private static final String oLON = "oLon";
    private static final String dLat = "dLat";
    private static final String dLon = "dLon";

    private static final String tableStructure = "CREATE TABLE " + TABLE + "(" +
            ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " + ORIGIN + " TEXT," + DESTINATION + " TEXT, " +
            DISTANCE + " TEXT, " + DURATION + " TEXT, " + oLAT + " FLOAT, " + oLON + " FLOAT," + dLat + " FLOAT, " +
            dLon + " FLOAT)";

    private static final String TAG = FavoritesCache.class.getSimpleName();
    private SQLiteDatabase database;
    private Cursor cursor;

    public FavoritesCache(Context context) {
        super(context, DATABASE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(tableStructure);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public List<FavoritesCacheModel> getFavorites(){

        FavoritesCacheModel favoritesCacheModel;
        List<FavoritesCacheModel> favoritesCacheModelList = new ArrayList<>();

        database = this.getReadableDatabase();

        if (database.isOpen()) {

            String sqlQuery = "SELECT * FROM " + TABLE;
            cursor = database.rawQuery(sqlQuery, null);

            if ((cursor != null) && (!cursor.isClosed())){

                if (cursor.isLast()) {
                    cursor.moveToFirst();
                }

                try{
                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            String origin = cursor.getString(cursor.getColumnIndex(ORIGIN)).replace("-", "'");
                            String destination = cursor.getString(cursor.getColumnIndex(DESTINATION)).replace("-", "'");
                            favoritesCacheModel = new FavoritesCacheModel(
                                    origin,
                                    destination,
                                    cursor.getString(cursor.getColumnIndex(DISTANCE)),
                                    cursor.getString(cursor.getColumnIndex(DURATION)),
                                    cursor.getDouble(cursor.getColumnIndex(oLAT)),
                                    cursor.getDouble(cursor.getColumnIndex(oLON)),
                                    cursor.getDouble(cursor.getColumnIndex(dLat)),
                                    cursor.getDouble(cursor.getColumnIndex(dLon))
                                    );
                            favoritesCacheModelList.add(favoritesCacheModel);
                        }
                    }
                }catch(SQLiteException e){
                    Log.v(TAG, e.getLocalizedMessage());
                }

                finally {
                    cleanUP();
                }
            }
        }

        return favoritesCacheModelList;
    }

    public boolean setFavorites(FavoritesCacheModel favoritesCacheModel) {

        database = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();

        String origin = favoritesCacheModel.getOrigin().replace("'", "-");
        String destination = favoritesCacheModel.getDestination().replace("'", "-");


        final String selectSql = "SELECT * FROM " + TABLE + " WHERE " + ORIGIN + "='"+ origin + "'" +
                " AND " + DESTINATION + "='" + destination + "'";

        if(database.isOpen()){
            cursor = database.rawQuery(selectSql, null);
            if(cursor != null) {

                try {
                    if (cursor.getCount() <= 0) {
                        if (cursor.isLast()) {
                            cursor.moveToFirst();
                        }

                        if (database.isReadOnly()) {
                            database = this.getWritableDatabase();
                        }

                        contentValues.put(ORIGIN, origin);
                        contentValues.put(DESTINATION, destination);
                        contentValues.put(DISTANCE, favoritesCacheModel.getDistance());
                        contentValues.put(DURATION, favoritesCacheModel.getDuration());
                        contentValues.put(oLAT, favoritesCacheModel.getOriginLatitude());
                        contentValues.put(oLON, favoritesCacheModel.getOriginLongitude());
                        contentValues.put(dLat, favoritesCacheModel.getDestinationLatitude());
                        contentValues.put(dLon, favoritesCacheModel.getDestinationLongitude());
                        long result = database.insert(TABLE, null, contentValues);

                        return result != -1;
                    }
                }

                catch (SQLiteException e) {
                    Log.v(TAG, e.getLocalizedMessage());
                }

                finally {

                    if(contentValues != null) {
                        contentValues.clear();
                    }

                    cleanUP();
                }
            }
        }
        return false;
    }

    private void cleanUP() {

        if(cursor != null) {
            cursor.close();
        }

        if(database != null) {
            database.close();
        }
    }
}

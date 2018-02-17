package com.example.jamessmith.trackerappexample1.filemanagement;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.jamessmith.trackerappexample1.favorites.FavoritesModel;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by James Smith on 13/02/2018.
 */

public class FileManager {

    private static final String TAG = FileManager.class.getSimpleName();
    private static final String uri = "trackedData.txt";
    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private List<FavoritesModel> favoritesModelList = new ArrayList<>();
    private Gson gson;

    public FileManager(Context context) {
        this.context = context;
        sharedPreferences = this.context.getSharedPreferences(uri, 0); // 0 - Private mode
        editor = sharedPreferences.edit();
        gson = new Gson();
    }

    public void setFavoritesList(FavoritesModel favoritesModel) {

           try {
               for(int i = 0; i < favoritesModelList.size(); i ++) {

                   editor.putString("origin", favoritesModel.getOrigin());
                   editor.putString("destination",favoritesModel.getDestination());
                   editor.putString("duration", favoritesModel.getDuration());
                   editor.putString("distance", favoritesModel.getDistance());
                   editor.commit();
               }

               editor.clear();

           } catch (Exception e) {
               Log.v(TAG, e.getLocalizedMessage());
           }
    }

    public List<FavoritesModel> getFavoritesList() {

        FavoritesModel favoritesModel;

        try {

            favoritesModel = new FavoritesModel(
                    sharedPreferences.getString("origin", null),
                    sharedPreferences.getString("destination", null),
                    sharedPreferences.getString("duration", null),
                    sharedPreferences.getString("distance", null)
            );

            favoritesModelList.add(favoritesModel);
        }catch(Exception e) {
            Log.v(TAG, e.getLocalizedMessage());
        }

        return favoritesModelList;
    }
}

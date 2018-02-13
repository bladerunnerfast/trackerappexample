package com.example.jamessmith.trackerappexample1.filemanagement;

import android.content.Context;
import android.util.Log;

import com.example.jamessmith.trackerappexample1.favorites.FavoritesModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by James Smith on 13/02/2018.
 */

public class FileManager {

    private static final String TAG = FileManager.class.getSimpleName();
    private static final String uri = "trackedData.txt";
    private Context context;

    public FileManager(Context context) {
        this.context = context;
    }

    public void setFavoritesList(FavoritesModel favoritesModel) {

       String data = favoritesModel.getOrigin() + "," + favoritesModel.getDistance() + "," +
                favoritesModel.getDistance() + "," + favoritesModel.getDuration();
        try {

            OutputStream outputStream = context.openFileOutput(uri, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());

            Log.v(TAG, "Written to file");
            outputStream.close();

        } catch (Exception e) {
            Log.v(TAG, e.getLocalizedMessage());
        }
    }

    public List<FavoritesModel> getFavoritesList() {

        FavoritesModel favoritesModel;
        List<FavoritesModel> favoritesModelList = new ArrayList<>();

        try {

            FileInputStream fileInputStream = context.openFileInput(uri);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            StringTokenizer stringTokenizer;

            while ((line = bufferedReader.readLine()) != null) {
                stringTokenizer = new StringTokenizer(line, ",");
                favoritesModel = new FavoritesModel(stringTokenizer.nextToken(), stringTokenizer.nextToken(),
                        stringTokenizer.nextToken(), stringTokenizer.nextToken());
                favoritesModelList.add(favoritesModel);
            }

            Log.v(TAG, "Read from file");
            fileInputStream.close();
            inputStreamReader.close();
            bufferedReader.close();

        }catch(Exception e) {
            Log.v(TAG, e.getLocalizedMessage());
        }

        return favoritesModelList;
    }
}

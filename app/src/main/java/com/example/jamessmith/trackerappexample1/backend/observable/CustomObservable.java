package com.example.jamessmith.trackerappexample1.backend.observable;

import com.example.jamessmith.trackerappexample1.backend.model.GoogleDataModel;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 *
 *
 * Created by James Smith on 10/02/2018.
 */

public interface CustomObservable {

    @GET("/maps/api/directions/json?")
    Observable<GoogleDataModel> getMapData(@Query("key") String key, @Query("origin") String origin, @Query("destination") String destination,
                                           @Query("sensor") String sensor, @Query("mode") String mode, @Query("traffic") String traffic);
}

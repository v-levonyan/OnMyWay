package com.example.vahanl.onmyway;

import android.app.Application;

import com.google.maps.GeoApiContext;

/**
 * Created by vahanl on 9/12/17.
 */

public class MyApplication extends Application {


    private GeoApiContext geoApiContext;
    @Override
    public void onCreate() {
        super.onCreate();

        geoApiContext = new GeoApiContext.Builder()
                .apiKey("AIzaSyC1hc-oW8dFFQ4tGzRifkH_CtgluJKcTAk")
                .build();
    }

    public GeoApiContext getGeoApiContext() {
        return geoApiContext;
    }
}

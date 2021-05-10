package com.example.RaceApp;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    private static MyApplication singleton;

    private List<Location> myLocations;

    public Location currentLocation;
    public FusedLocationProviderClient fusedLocationProviderClient;
    public LocationRequest locationRequest;
    public LocationCallback locationCallBack;


    public MyApplication getInstance(){
        return singleton;
    }

    public void onCreate(){
        super.onCreate();
        singleton = this;

    }



    public List<Location> getMyLocations() {
        return myLocations;
    }

    public void setMyLocations(List<Location> myLocations) {
        this.myLocations = myLocations;
    }
}

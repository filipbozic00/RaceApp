package com.example.RaceApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.service.controls.templates.TemperatureControlTemplate;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;


public class MainActivity extends AppCompatActivity {

    public static final int UPDATE_INTERVAL = 1000;
    public static final int FASTEST_INTERVAL = 500;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, accelerateTime;
    Switch sw_locationupdates, sw_gps;

    //APIfor location services
    FusedLocationProviderClient fusedLocationProviderClient;
    boolean updateOn = false; //variable to remember if we are tracking location on not
    LocationRequest locationRequest; // config file for all settings related to FusedLocationProviderClient
    LocationCallback locationCallBack;

    double speed;
    double latitude;
    double longditude;

    MyApplication myApplication;

    Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        sw_locationupdates = findViewById(R.id.sw_locationupdates);
        sw_gps = findViewById(R.id.sw_gps);
        accelerateTime = findViewById(R.id.accelerateTime);
        myApplication = (MyApplication) getApplication();




        //EventBus.getDefault().register(this);
        //set all properties of LocationReq

        locationRequest = LocationRequest.create();

        locationRequest.setInterval(UPDATE_INTERVAL);

        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();
                updateUIValues(location);
            }
        };

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sw_gps.isChecked()){
                    //high accuracy=GPS
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensor");
                }
                else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using cellular network + WiFi");
                }
            }
        });

        //updateGPS();

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sw_locationupdates.isChecked()){
                    // turn on location tracking

                    updateGPS();
                }
                else{
                    //turn off tracking
                    stopLocationUpdates();
                }
            }
        });


        //updateGPS();
    } //END onCreate


    //@SuppressLint("MissingPermission")
    private void stopLocationUpdates() {

        tv_updates.setText("Not tracked");
        tv_lat.setText("Not tracked");
        tv_lon.setText("Not tracked");
        tv_speed.setText("Not tracked");
        tv_altitude.setText("Not tracked");
        tv_sensor.setText("Not tracked");
        tv_accuracy.setText("Not tracked");
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");
        //fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);

        updateGPS();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            switch (requestCode){
                case PERMISSIONS_FINE_LOCATION:
                    if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        updateGPS();
                    }
                    else {
                        Toast.makeText(this, "This app requres GPS to work roperly", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
            }
        }

        catch(Exception e){
            Toast.makeText(this, "This app requres GPS to work roperly: " + e, Toast.LENGTH_SHORT).show();
        }
    }



    @SuppressLint("MissingPermission")
    private void updateGPS(){
            // get permissions from ndroid user to track GPS
            // get current location
            // update UI
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
            myApplication.fusedLocationProviderClient = fusedLocationProviderClient;
            myApplication.locationRequest = locationRequest;
            myApplication.locationCallBack = locationCallBack;
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                // user provided permission
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // we got perrmissions. Put the values of location into UI
                        updateUIValues(location);
                        CustomMessageEvent event = new CustomMessageEvent();
                        event.setLati(latitude);
                        event.setLongi(longditude);
                        event.setLoc(location);
                        event.setLocationRequest(locationRequest);
                        event.setLocationCallback(locationCallBack);
                        EventBus.getDefault().postSticky(event);

                        /*Intent i = new Intent(getBaseContext(), MyMap.class);
                        i.putExtra("longditude", location.getLongitude());
                        i.putExtra("latitude", location.getLatitude());
                        startActivityForResult(i, MyMap.ACTIVITY_ID);*/

                        myApplication.currentLocation=location;

                    }
                });
            }
            else {
                // permission not granted
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
                }
            }

    }

    private void updateUIValues(Location location) {
        // update all the text view objects with a new location

            try{
                latitude = location.getLatitude();
                tv_lat.setText(String.valueOf(latitude));
            }
            catch (Exception eLat){
                Toast.makeText(this, "Latitude problem " + eLat, Toast.LENGTH_SHORT).show();
            }
            try {
                longditude = location.getLongitude();
                tv_lon.setText(String.valueOf(longditude));
            }
            catch (Exception eLon){
                Toast.makeText(this, "Longditude problem " + eLon, Toast.LENGTH_SHORT).show();
            }
            try {
                tv_accuracy.setText(String.valueOf(location.getAccuracy()));
            }
            catch (Exception eAcc){
                Toast.makeText(this, "Accuracy problem " + eAcc, Toast.LENGTH_SHORT).show();
            }

            try{
                if(location.hasSpeed()){
                    speed = location.getSpeed()* 18 / 5; // because getSpeed returns meters per second, calculating it to get km/h
                    tv_speed.setText(new DecimalFormat("#.##").format(speed) + " km/hr");
                }
                else tv_speed.setText("Not available");
            }
            catch (Exception eSpd){
                Toast.makeText(this, "Speed problem " + eSpd, Toast.LENGTH_SHORT).show();
            }
            try{
                if(location.hasAltitude()){
                    tv_altitude.setText(String.valueOf(location.getAltitude()));
                }
                else tv_altitude.setText("Not available");
            }
            catch (Exception eAlt){
                Toast.makeText(this, "Speed problem " + eAlt, Toast.LENGTH_SHORT).show();
            }

    }


    public void onClickOpenMap(View view) {
        Intent i = new Intent(getBaseContext(), MyMap.class);
        startActivity(i);
    }
}

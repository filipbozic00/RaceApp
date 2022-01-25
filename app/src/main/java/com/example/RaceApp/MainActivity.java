package com.example.RaceApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.service.controls.templates.TemperatureControlTemplate;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {

    public static final int UPDATE_INTERVAL = 1000;
    public static final int FASTEST_INTERVAL = 500;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    private static final String TAG = MainActivity.class.getSimpleName();

    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, accelerateTime, timer, lati, longi;
    Switch sw_locationupdates, sw_gps;
    Button btn, stopBtn;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds ;
    int baseNum = 0;

    //APIfor location services
    FusedLocationProviderClient fusedLocationProviderClient;
    boolean updateOn = false; //variable to remember if we are tracking location on not
    LocationRequest locationRequest; // config file for all settings related to FusedLocationProviderClient
    LocationCallback locationCallBack;

    double speed;
    double latitude;
    double longditude;
    double lat, lon;

    MyApplication myApplication;

    Location currentLocation;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;


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
        btn = findViewById(R.id.timer_button);
        stopBtn = findViewById(R.id.stop_button);
        timer = findViewById(R.id.timerView);
        lati = findViewById(R.id.textView2);
        longi = findViewById(R.id.textView3);



        firebaseDatabase = FirebaseDatabase.getInstance();



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
                    startLocationUpdates();
                    //updateGPS();
                }
                else{
                    //turn off tracking
                    stopLocationUpdates();
                }
            }
        });

        lapTime();
        updateGPS();
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
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);

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
            //fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
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
                        //lapTime(location);

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


    public void getLocForChrono() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // user provided permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                }
            });
        }
    }

    public void lapTime(){
        handler = new Handler();

        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                StartTime = SystemClock.uptimeMillis();
                getLocForChrono();
                handler.postDelayed(runnable, 0);
                lati.setText(String.valueOf(round(lat, 5)));
                longi.setText(String.valueOf(round(lon, 5)));
                //Log.i(TAG,"Longditude: "+round(longditude, 5));
                //Log.i(TAG,"Longditude: "+round(longditude, 5));

                if(round(latitude, 5) == round(lat, 5) && round(longditude, 5) == round(lon, 5) && speed > 3){
                    Log.i(TAG,"Longditudeeee: "+round(longditude, 3));
                    stopTimer();
                }

            }
        });
        //euklidska razdalja

       /* Log.i(TAG,"Longditude: "+lon);
        if((int)latitude == (int)lat && (int)longditude == (int)lon){
            Log.i(TAG,"Longditudeeee: "+lon);
            stopTimer();
        }*/

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                   stopTimer();

            }

        });
    }

    public void stopTimer(){
        TimeBuff += MillisecondTime;
        String timebuffString = ("" + Minutes + ":"
                + String.format("%02d", Seconds) + ":"
                + String.format("%03d", MilliSeconds));
        handler.removeCallbacks(runnable);
        baseNum=baseNum+1;
        Log.i(TAG, "Value of baseNum"+baseNum);
        databaseReference = firebaseDatabase.getReference("Lap times "+String.valueOf(baseNum));
        addDataToFirebase(timebuffString);
        Context context = getApplicationContext();
        notification("Lap time", timebuffString, context);
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            timer.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));


            handler.postDelayed(this, 0);
        }

    };

    public void notification(String title, String message, Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = createID();
        String channelId = "channel-id";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)//R.mipmap.ic_launcher
                .setContentTitle(title)
                .setContentText(message)
                .setVibrate(new long[]{100, 250})
                .setLights(Color.YELLOW, 500, 5000)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.purple_200));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(new Intent(context, MainActivity.class));
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }

    public int createID() {
        Date now = new Date();
        int id = Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.FRENCH).format(now));
        return id;
    }

    /*private void addNotification() {
        // Builds your notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Lap time: ")
                .setContentText("A video has just arrived!");

        // Creates the intent needed to show the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }*/

    public void onClickOpenMap(View view) {
        Intent i = new Intent(getBaseContext(), MyMap.class);
        startActivity(i);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void addDataToFirebase(String laptime){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                databaseReference.setValue(laptime);
                Toast.makeText(MainActivity.this, "Data added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Data failed to add", Toast.LENGTH_SHORT).show();
            }
        });
    }



    /*@Override
    public void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }*/
}

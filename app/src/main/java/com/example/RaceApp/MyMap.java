package com.example.RaceApp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import org.greenrobot.eventbus.ThreadMode;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.Random;

public class MyMap extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int ACTIVITY_ID = 113;
    private static final int PERMISSION_ALL = 123;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    public static final int UPDATE_INTERVAL = 1000;
    public static final int FASTEST_INTERVAL = 500;

    MapView map = null;
    GeoPoint startPoint;
    IMapController mapController;
    Marker marker;
    Random rnd;
    Polyline path;
    Location locc;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallBack;

    int baseNum = 100;

    //CustomMessageEvent event;

    double latit;
    double longdit;

    MyApplication app;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //private ArrayList<LatLng> points;

    TextView Lon;
    TextView Lat;
    ToggleButton tbOk;
    Button btn, btn2;
    Switch aSwitch;

    Handler handler;

    String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.CAMERA
    };
    Location lok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_my_map);
        btn = findViewById(R.id.updateGPSbtn);
        aSwitch = findViewById(R.id.switchLoop);
        btn2 = findViewById(R.id.stopGPSupdate);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Lap times "+String.valueOf(baseNum));
        addDataToFirebase("blaaaa");


        app = (MyApplication) getApplication();

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        //points = new ArrayList<LatLng>();
        Lat = findViewById(R.id.Lat);
        Lon = findViewById(R.id.Lon);
        //tbOk = findViewById(R.id.tbOn);
        rnd = new Random();

        locationRequest = LocationRequest.create();

        locationRequest.setInterval(UPDATE_INTERVAL);

        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();
                initMapStartGPS(location);
                //updateGPS();
            }
        };


        aSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(aSwitch.isChecked()){
                    startLocationUpdates();
                }
                else stopLocationUpdates();
            }
        });

        EventBus.getDefault().register(this);

        /*btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateGPS();
            }
        });*/
        /*
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            int i =1;
            @Override
            public void run() {
                updateGPS();
                i++;
                //Log.i(TAG, "Primer"+i);
                handler.postDelayed(this, 500);
            }
        }, 500);*/


        startMap();
        //onEvent();
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    public void startMap(){
        handler = new Handler();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.postDelayed(runnable, 0);

            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable);
                //handler.postDelayed(runnable, 5000);
                fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
            }
        });

    }

    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            updateGPS();
            handler.postDelayed(this, 0);
            if(btn2.isPressed()){
                handler.removeCallbacks(this);
                fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
            }
        }
    };

    protected void onStop(){
        super.onStop();
        handler.removeCallbacks(runnable);

    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(CustomMessageEvent event){
        //Log.i(TAG, "onMessageEvent"+ event.getLati());
        latit = event.getLati();
        longdit = event.getLongi();
        locc = event.getLoc();
        locationRequest = event.getLocationRequest();
        locationCallBack = event.getLocationCallback();
        //Lat.setText(String.valueOf(latit));
    }


    private void addDataToFirebase(String laptime){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                databaseReference.setValue(laptime);
                Toast.makeText(MyMap.this, "Data added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyMap.this, "Data failed to add", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void updateGPS(){
        // get permissions from ndroid user to track GPS
        // get current location
        // update UI
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MyMap.this);


        //fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            // user provided permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // we got perrmissions. Put the values of location into UI
                    initMapStartGPS(location);

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

    @SuppressLint("MissingPermission")
    public void initMapStartGPS(Location loc) {


        /*app.fusedLocationProviderClient.requestLocationUpdates(app.locationRequest, app.locationCallBack, null);
        app.fusedLocationProviderClient.getLastLocation();-*/
        Bundle getExtras = getIntent().getExtras();
        //if(getExtras != null) loc = getParceableExtra("LOCATION");

        //double lat = app.currentLocation.getLatitude();
        //double lat = getExtras.getDouble("latitude");
        try{
            mapController = map.getController();
            mapController.setZoom(18.5);
            double lat = loc.getLatitude();
            Lat.setText(String.valueOf(lat));

            //double lon = getExtras.getDouble("longditude");
            //double lon = app.currentLocation.getLongitude();
            double lon = loc.getLongitude();
            Lon.setText(String.valueOf(lon));

            startPoint = new GeoPoint(lat,lon);
            mapController.setCenter(startPoint);
            getPositionMarker().setPosition(startPoint);
            map.invalidate();
            GeoPoint gPt0 = new GeoPoint(44.82951, 17.20870);
            GeoPoint gPt1 = new GeoPoint(42.527621d, 18.20870);
            Polyline line = new Polyline();
            line .addPoint(gPt0);
            line .addPoint(gPt1);
            map.getOverlays().add(line);
        }
        catch (Exception e){
          Toast.makeText(this, "location problem", Toast.LENGTH_SHORT).show();
        }
        //double lat = loc.getLatitude();


    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this,PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {

            updateGPS();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
        finish();
    }

    /*
     Helper function to check permissions
      */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_ALL: {
                if (grantResults.length >= PERMISSIONS.length) {
                    for (int i=0; i<PERMISSIONS.length; i++) {
                        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this,"NimaÅ¡ vseh dovolenj!",Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                    updateGPS();
                }
                else
                    finish();
            }

        }
    }


    private Polyline getPath() { //Singelton
        if (path==null) {
            path = new Polyline();
            path.setColor(Color.RED);
            path.setWidth(10);
            path.addPoint(startPoint.clone());
            map.getOverlayManager().add(path);
        }
        return path;
    }
    private Marker getPositionMarker() { //Singelton
        if (marker==null) {
            marker = new Marker(map);
            marker.setTitle("Here I am");
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(getResources().getDrawable(R.drawable.ic_position));
            map.getOverlays().add(marker);
        }
        return marker;
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause(); //needed for compass, my location overlays, v6.0.0 and up
        Log.i(TAG,"onPause "+lok);
    }

    public void onResume(){
        super.onResume();
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }




    /*public void onClickDraw1(View view) {
        startPoint.setLatitude(startPoint.getLatitude()+(rnd.nextDouble()-0.5)*0.001);
        mapController.setCenter(startPoint);
        getPositionMarker().setPosition(startPoint);
        map.invalidate();
    }

    public void onClickDraw2(View view) {
        startPoint.setLatitude(startPoint.getLatitude()+(rnd.nextDouble()-0.5)*0.001);
        mapController.setCenter(startPoint);
        Polygon circle = new Polygon(map);
        circle.setPoints(Polygon.pointsAsCircle(startPoint, 40.0+rnd.nextInt(100)));
        circle.getFillPaint().setColor(0x32323232);
        circle.getOutlinePaint().setColor(Color.GREEN);
        circle.getOutlinePaint().setStrokeWidth(2);
        circle.setTitle("Area title");
        map.getOverlays().add(circle); //Duplicate every time new
        map.invalidate();
    }

    public void onClickDraw3(View view) {
        CompassOverlay mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);
        map.invalidate();
    }
    public void onClickDraw4(View view) {
        //Polyline path = new Polyline();
        startPoint.setLatitude(startPoint.getLatitude()+(rnd.nextDouble()-0.5)*0.001);
        startPoint.setLongitude(startPoint.getLongitude()+(rnd.nextDouble()-0.5)*0.001);
        getPath().addPoint(startPoint.clone());
        map.invalidate();
    }

    public void onClickTrackGPSOn(View view) {
        Log.i(TAG,"Status GPS:"+ tbOk.isChecked());
        //TODO
    }*/
}
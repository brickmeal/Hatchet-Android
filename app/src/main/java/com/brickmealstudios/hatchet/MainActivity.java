package com.brickmealstudios.hatchet;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.SceneView;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    public static PinpointManager pinpointManager;
    private SceneView mSceneView;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private double altitude = 20.0;
    private double pitch = 70.0;
    private double roll = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                Log.d("YourMainActivity", "AWSMobileClient is instantiated and you are connected to AWS!");
            }
        }).execute();

        PinpointConfiguration config = new PinpointConfiguration(
                MainActivity.this,
                AWSMobileClient.getInstance().getCredentialsProvider(),
                AWSMobileClient.getInstance().getConfiguration()
        );
        pinpointManager = new PinpointManager(config);
        pinpointManager.getSessionClient().startSession();
        pinpointManager.getAnalyticsClient().submitEvents();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSceneView = findViewById(R.id.mapView);
        setupMap();
        setupLocation();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdates();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSceneView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSceneView.resume();
    }

    @Override
    protected void onDestroy() {
        mSceneView.dispose();
        super.onDestroy();
    }

    private void setupMap() {
        if (mSceneView != null) {
            ArcGISScene scene = new ArcGISScene();
            scene.setBasemap(Basemap.createStreets());
            mSceneView.setScene(scene);
        }
    }

    private void setupLocation() {
        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double deviceLat = location.getLatitude();
                double deviceLng = location.getLongitude();
                double deviceBrng = location.getBearing();
                HashMap<String, Double> cameraPos = calcCameraPosition(deviceLat, deviceLng, deviceBrng);
                double camLat = cameraPos.getOrDefault("lat", deviceLat);
                double camLng = cameraPos.getOrDefault("lng", deviceLng);
                Log.d("YourMainActivity", String.format("Lat: %s, Lng: %s, Brng: %s", Double.toString(camLat), Double.toString(camLng), Double.toString(deviceBrng)));
                Camera camera = new Camera(deviceLat, deviceLng, altitude, deviceBrng, pitch, roll);
                mSceneView.setViewpointCamera(camera);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
    }

    private double calcBackBearing(double bearing) {
        if (bearing >= 180.0) {
            return bearing - 180.0;
        } else {
            return bearing + 180.0;
        }
    }

    private HashMap<String, Double> calcCameraPosition(double deviceLat, double deviceLng, double deviceBrng) {
        // Radius of the earth
        double R = 6378.1;
        // How far behind the player to place the camera (in km)
        double DeviceCameraGap = 200.0 / 1000.0;

        double deviceLatRadians = Math.toRadians(deviceLat);
        double deviceLngRadians = Math.toRadians(deviceLng);
        double backBearing = calcBackBearing(deviceBrng);

        double backLatRadians = Math.asin(Math.sin(deviceLatRadians) * Math.cos(DeviceCameraGap/R) +
                Math.cos(deviceLatRadians) * Math.sin(DeviceCameraGap/R) * Math.cos(backBearing));

        double backLngRadians = deviceLngRadians + Math.atan2(Math.sin(backBearing) * Math.sin(DeviceCameraGap/R) * Math.cos(deviceLatRadians),
                Math.cos(DeviceCameraGap/R) - Math.sin(deviceLatRadians) * Math.sin(backLatRadians));

        HashMap hm = new HashMap();
        hm.put("lat", Math.toDegrees(backLatRadians));
        hm.put("lng", Math.toDegrees(backLngRadians));
        return hm;
    }

    private void startLocationUpdates() {
        Log.d("YourMainActivity", "Start Location Updates Button Pressed");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("YourMainActivity", "Should Show Request Permission Rationale");
            } else {
                Log.d("YourMainActivity", "Requesting location access permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        } else {
            // Register the listener with the Location Manager to receive location updates
            Log.d("YourMainActivity", "Registering location update listener");
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 2, mLocationListener);
        }
    }
}
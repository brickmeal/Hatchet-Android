package com.brickmealstudios.hatchet.types;

import android.location.Location;

public class Telemetry {
    private double latitude;
    private double longitude;
    private double altitude;
    private float bearing;
    private float accuracy;
    private float bearingAccuracyDegrees;
    private float speed;
    private String provider;
    private float speedAccuracyMetersPerSecond;
    private float verticalAccuracyMeters;
    private long time;

    public Telemetry(Location loc) {
        this.latitude = loc.getLatitude();
        this.longitude = loc.getLongitude();
        this.altitude = loc.getAltitude();
        this.bearing = loc.getBearing();
        this.accuracy = loc.getAccuracy();
        this.bearingAccuracyDegrees = loc.getBearingAccuracyDegrees();
        this.speed = loc.getSpeed();
        this.provider = loc.getProvider();
        this.speedAccuracyMetersPerSecond = loc.getSpeedAccuracyMetersPerSecond();
        this.verticalAccuracyMeters = loc.getVerticalAccuracyMeters();
        this.time = loc.getTime();
    }
}

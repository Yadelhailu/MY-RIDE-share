package com.example.myrideshare;

public class RideRequest {
    public double latitude;
    public double longitude;
    public long timestamp;

    public RideRequest() {} // Required for Firebase

    public RideRequest(double latitude, double longitude, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
}

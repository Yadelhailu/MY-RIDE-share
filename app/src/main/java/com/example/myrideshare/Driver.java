package com.example.myrideshare;

public class Driver {
    public String name;
    public double latitude;
    public double longitude;

    public Driver() {} // Required for Firebase or future use

    public Driver(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

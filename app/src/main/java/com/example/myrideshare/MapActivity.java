package com.example.myrideshare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private MapView mapView;
    private Button requestRideButton;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    private DatabaseReference rideRequestRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        // Firebase initialization
        mAuth = FirebaseAuth.getInstance();
        rideRequestRef = FirebaseDatabase.getInstance().getReference("rideRequests");

        // UI init
        mapView = findViewById(R.id.map);
        requestRideButton = findViewById(R.id.requestRideButton);
        requestRideButton.setEnabled(false); // Disabled until location is loaded

        // Map setup
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestPermissionsIfNecessary();

        // Ride button click
        requestRideButton.setOnClickListener(v -> {
            if (currentLocation != null) {
                String userId = mAuth.getCurrentUser().getUid();
                double lat = currentLocation.getLatitude();
                double lon = currentLocation.getLongitude();
                long timestamp = System.currentTimeMillis();

                RideRequest request = new RideRequest(lat, lon, timestamp);

                rideRequestRef.child(userId).setValue(request)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, "Ride requested!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show());

                showNearbyDrivers(); // Show drivers only after request
            } else {
                Toast.makeText(this, "Location not available yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestPermissionsIfNecessary() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                            showLocationOnMap(location);
                            requestRideButton.setEnabled(true); // Enable only after location loads
                        } else {
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showLocationOnMap(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        GeoPoint userLocation = new GeoPoint(latitude, longitude);
        mapView.getController().setZoom(18.0);
        mapView.getController().setCenter(userLocation);

        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(userLocation);
        userMarker.setTitle("You are here");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(userMarker);

        mapView.invalidate(); // Refresh
    }

    private void showNearbyDrivers() {
        if (currentLocation == null) {
            Toast.makeText(this, "Can't show drivers without location", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove old driver markers if needed
        mapView.getOverlays().removeIf(overlay -> {
            if (overlay instanceof Marker) {
                Marker m = (Marker) overlay;
                return m.getTitle() != null && !m.getTitle().equals("You are here");
            }
            return false;
        });

        Driver[] drivers = new Driver[] {
                new Driver("Alice - Toyota", currentLocation.getLatitude() + 0.002, currentLocation.getLongitude() + 0.002),
                new Driver("Bob - Honda", currentLocation.getLatitude() - 0.0015, currentLocation.getLongitude() + 0.001),
                new Driver("Charlie - Tesla", currentLocation.getLatitude() + 0.001, currentLocation.getLongitude() - 0.002)
        };

        for (Driver driver : drivers) {
            GeoPoint point = new GeoPoint(driver.latitude, driver.longitude);
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(driver.name);
            marker.setSnippet("Available for rides");
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }
}

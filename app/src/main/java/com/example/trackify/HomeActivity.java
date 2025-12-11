package com.example.trackify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;

import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST = 2001;

    private EditText etDrop;
    private TextView tvStartPoint;
    private Button btnStart, btnMaintenance, btnLoginTop;

    private double originLat = 0;
    private double originLon = 0;

    private LocationManager lm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        etDrop = findViewById(R.id.etDrop);
        tvStartPoint = findViewById(R.id.tvStartPoint);
        btnStart = findViewById(R.id.btnStart);
        btnMaintenance = findViewById(R.id.btnMaintenance);
        btnLoginTop = findViewById(R.id.btnLoginTop);

        btnMaintenance.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MaintenanceActivity.class))
        );
        btnLoginTop.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MainActivity.class))
        );

        requestLocationPermission();


        btnStart.setOnClickListener(v -> safeStartNavigation());
    }

    // ============= PERMISSION =============
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST
            );
        } else {
            getUserLocation();
        }
    }

    // ============= GET USER LOCATION =============
    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        try {
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);

            if (lm == null) {
                Toast.makeText(this, "LocationManager not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // أسرع: NETWORK
            lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    1,
                    locationListener
            );

            // أدق: GPS
            lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    3,
                    locationListener
            );

        } catch (Exception e) {
            Toast.makeText(this, "Location Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("LocationError", e.toString());
        }
    }

    // ============= LOCATION LISTENER =============
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {

            originLat = location.getLatitude();
            originLon = location.getLongitude();

            tvStartPoint.setText("Start: " + originLat + ", " + originLon);

            Log.d("GPS", "Location updated: " + originLat + ", " + originLon);
        }
    };

    // ============= SAFER START NAVIGATION WRAPPER =============
    private void safeStartNavigation() {
        try {
            handleStartNavigation();
        } catch (Exception e) {
            Toast.makeText(this, "Navigation Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("NavigationCrash", e.toString());
        }
    }

    // ============= START NAVIGATION LOGIC =============
    private void handleStartNavigation() {

        String dropText = etDrop.getText().toString().trim();

        if (dropText.isEmpty()) {
            etDrop.setError("Enter destination");
            return;
        }

        GeoPoint destination = getLocationFromAddress(dropText);

        if (destination == null) {
            Toast.makeText(this, "Cannot find this location", Toast.LENGTH_SHORT).show();
            return;
        }

        // تأكد أن الـ GPS اشتغل فعلياً
        if (originLat == 0.0 || originLon == 0.0) {
            Toast.makeText(this, "Still waiting for GPS...", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("ORIGIN", originLat + "," + originLon);
        Log.d("DEST", destination.getLatitude() + "," + destination.getLongitude());

        // أرسل الداتا
        Intent intent = new Intent(HomeActivity.this, BestPathActivity.class);
        intent.putExtra("origin_lat", originLat);
        intent.putExtra("origin_lon", originLon);
        intent.putExtra("dest_lat", destination.getLatitude());
        intent.putExtra("dest_lon", destination.getLongitude());

        startActivity(intent);
    }


    // ============= GEOCODER (DESTINATION) =============
    private GeoPoint getLocationFromAddress(String address) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> list = geocoder.getFromLocationName(address, 1);

            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                return new GeoPoint(a.getLatitude(), a.getLongitude());
            }

        } catch (Exception e) {
            Toast.makeText(this, "Geocoder error: check internet", Toast.LENGTH_SHORT).show();
            Log.e("GeocoderError", e.toString());
        }
        return null;
    }

    // ============= PERMISSION RESULT =============
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getUserLocation();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

package com.example.trackify;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

public class MaintenanceActivity extends AppCompatActivity {

    private TextView speedText, distanceText, maintenanceTimeText, oilText, tiresText, brakeText;

    private LocationManager locationManager;

    DistanceManager distanceManager = new DistanceManager();

    // mock values (replace with Firebase later)
    double lastOilKm = 120000;
    double lastTireKm = 110000;
    double lastBrakeKm = 125000;
    long lastMaintenanceDate = System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000); // 40 days ago

    private final int REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);

        // link UI
        speedText = findViewById(R.id.value_speed);
        distanceText = findViewById(R.id.value_distance);
        maintenanceTimeText = findViewById(R.id.value_maintenance);
        oilText = findViewById(R.id.value_oil);
        tiresText = findViewById(R.id.value_tires);
        brakeText = findViewById(R.id.value_brake);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Load initial totalKm (replace with Firebase value later)
        distanceManager.setInitialTotalKm(132500);

        // ask permissions
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE
        );

        startSpeedTracking();
        startMockDistanceGrowth(); // TEMPORARY until Google Maps is ready
    }

    // =====================
    // SPEED SYSTEM
    // =====================
    @SuppressLint("MissingPermission")
    private void startSpeedTracking() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Enable GPS permission", Toast.LENGTH_SHORT).show();
            return;
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                        float speed = location.getSpeed(); // m/s
                        float speedKmh = speed * 3.6f;     // convert to km/h

                        // update UI
                        speedText.setText(String.format(" %.1f km/h", speedKmh));
                    }
                }
        );
    }

    // =====================
    // TEMPORARY DISTANCE GENERATOR
    // =====================
    private void startMockDistanceGrowth() {
        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // add fake distance (replace with Google Maps real distance later)
                distanceManager.addDistance(0.1); // add 0.1 km

                updateDistanceUI();

                // run again
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    // Update all UI related to maintenance + distance
    private void updateDistanceUI() {

        double daily = distanceManager.getDailyKm();
        double total = distanceManager.getTotalKm();

        // UPDATE DISTANCE BOX
        distanceText.setText(String.format(" %.2f km", daily));

        // =====================
        // UPDATE MAINTENANCE
        // =====================

        // time-based maintenance
        int daysLeft = MaintenanceLogic.daysUntilMaintenance(lastMaintenanceDate);
        maintenanceTimeText.setText("Next maintenance: " + daysLeft + " days");

        // oil
        double oilLeft = MaintenanceLogic.remainingOil(total, lastOilKm);
        oilText.setText(String.format(" %.0f km left", oilLeft));

        // tires
        double tiresLeft = MaintenanceLogic.remainingTires(total, lastTireKm);
        tiresText.setText(String.format(" %.0f km left", tiresLeft));

        // brakes
        double brakesLeft = MaintenanceLogic.remainingBrakes(total, lastBrakeKm);
        brakeText.setText(String.format(" %.0f km left", brakesLeft));
    }
}

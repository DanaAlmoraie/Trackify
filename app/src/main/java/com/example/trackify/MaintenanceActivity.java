package com.example.trackify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MaintenanceActivity extends AppCompatActivity {

    private TextView speedText, distanceText, maintenanceTimeText, oilText, tiresText, brakeText;
    private LocationManager locationManager;
    private ImageButton btnBackM;

    DistanceManager distanceManager = new DistanceManager();

    // mock last maintenance values (ثابتة حالياً)
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
        btnBackM = findViewById(R.id.btnBackM);

        btnBackM.setOnClickListener(v ->
                startActivity(new Intent(MaintenanceActivity.this, HomeActivity.class))
        );

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // اطلب صلاحيات GPS
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE
        );

        // ابدأ تتبع السرعة
        startSpeedTracking();

        // 🔥 حمّل totalKm الحقيقي من Firebase بدلاً من قيمة ثابتة
        loadTotalKmFromFirebase();
    }

    // =====================
    // 🔥 1) LOAD KM FROM FIREBASE
    // =====================
    private void loadTotalKmFromFirebase() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("totalKm");


        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    double totalKm = snapshot.getValue(Double.class);

                    // أرسل القيمة لـ DistanceManager
                    distanceManager.setInitialTotalKm(totalKm);

                    // حدّث الواجهة
                    updateDistanceUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MaintenanceActivity.this,
                        "Failed to load vehicle data",
                        Toast.LENGTH_SHORT).show();
            }
        });
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
                        float speedKmh = speed * 3.6f;     // km/h

                        speedText.setText(String.format(" %.1f km/h", speedKmh));
                    }
                }
        );
    }

    // =====================
    // UPDATE MAINTENANCE UI
    // =====================
    private void updateDistanceUI() {

        double total = distanceManager.getTotalKm();

        // مسافة اليوم (احتمال تستخدم لاحقاً)
        distanceText.setText(String.format(" %.2f km", total));

        // Time-based maintenance
        int daysLeft = MaintenanceLogic.daysUntilMaintenance(lastMaintenanceDate);
        maintenanceTimeText.setText("Next maintenance: " + daysLeft + " days");

        // Oil
        double oilLeft = MaintenanceLogic.remainingOil(total, lastOilKm);
        oilText.setText(String.format(" %.0f km left", oilLeft));

        // Tires
        double tiresLeft = MaintenanceLogic.remainingTires(total, lastTireKm);
        tiresText.setText(String.format(" %.0f km left", tiresLeft));

        // Brakes
        double brakesLeft = MaintenanceLogic.remainingBrakes(total, lastBrakeKm);
        brakeText.setText(String.format(" %.0f km left", brakesLeft));
    }
}

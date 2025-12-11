package com.example.trackify;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class BestPathActivity extends FragmentActivity {

    private MapView mapView;
    private TextView tvSpeedValue, tvDistanceValue, tvStartPoint;
    private MaterialButton btnMaintenance;

    private OkHttpClient client = new OkHttpClient();

    private GeoPoint origin = null;
    private GeoPoint destination = null;

    private Polyline routePolyline;
    private Marker userMarker;

    private LocationManager locationManager;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_best_path);

        mapView = findViewById(R.id.osmMapView);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        tvDistanceValue = findViewById(R.id.tvDistanceValue);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v ->
                startActivity(new Intent(BestPathActivity.this, HomeActivity.class))
        );
        // GET DATA FROM HOME
        double oLat = getIntent().getDoubleExtra("origin_lat", 0);
        double oLon = getIntent().getDoubleExtra("origin_lon", 0);
        double dLat = getIntent().getDoubleExtra("dest_lat", 0);
        double dLon = getIntent().getDoubleExtra("dest_lon", 0);

        origin = new GeoPoint(oLat, oLon);
        destination = new GeoPoint(dLat, dLon);


        setupMap();
        startSpeedUpdates();
        drawRoute();
        Log.d("BEST_PATH", "origin=" + origin.getLatitude() + "," + origin.getLongitude());
        Log.d("BEST_PATH", "dest=" + destination.getLatitude() + "," + destination.getLongitude());


    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController controller = mapView.getController();
        controller.setZoom(15.0);
        controller.setCenter(origin);
    }

    private void updateTotalKmInFirebase(double tripKm) {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("totalKm");

        ref.get().addOnSuccessListener(snapshot -> {

            double oldKm = 0;

            if (snapshot.exists()) {
                oldKm = snapshot.getValue(Double.class);
            }

            double newTotal = oldKm + tripKm;

            ref.setValue(newTotal);

        }).addOnFailureListener(e -> {
            Log.e("FIREBASE", "Failed to read old KM");
        });
    }



    private void drawRoute() {

        String url = "https://router.project-osrm.org/route/v1/driving/"
                + origin.getLongitude() + "," + origin.getLatitude() + ";"
                + destination.getLongitude() + "," + destination.getLatitude()
                + "?overview=full&geometries=geojson";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(BestPathActivity.this, "Route failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (!response.isSuccessful()) return;

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray routeArr = json.getJSONArray("routes");
                    JSONObject route = routeArr.getJSONObject(0);

                    // *********** NEW: distance + duration *************
                    double distanceMeters = route.getDouble("distance");
                    double durationSeconds = route.getDouble("duration");

                    double distanceKm = distanceMeters / 1000.0;
                    double durationMin = durationSeconds / 60.0;

                    // **************************************************

                    JSONArray coords = route.getJSONObject("geometry").getJSONArray("coordinates");

                    List<GeoPoint> points = new ArrayList<>();

                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray c = coords.getJSONArray(i);
                        points.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                    }

                    runOnUiThread(() -> {

                        // Show Distance
                        tvDistanceValue.setText(String.format(Locale.US, "%.1f km", distanceKm));
                        // Save trip distance into Firebase (add to totalKm)
                        updateTotalKmInFirebase(distanceKm);


                        // Show ETA
                        TextView tvEtaValue = findViewById(R.id.tvEtaValue);
                        tvEtaValue.setText(String.format(Locale.US, "%.0f min", durationMin));

                        // Draw route
                        routePolyline = new Polyline();
                        routePolyline.setPoints(points);
                        routePolyline.setColor(0xFFFF3B30);
                        routePolyline.setWidth(8f);

                        mapView.getOverlays().add(routePolyline);
                        mapView.invalidate();

                        // add origin marker
                        Marker startMarker = new Marker(mapView);
                        startMarker.setPosition(origin);
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        startMarker.setTitle("Start");
                        mapView.getOverlays().add(startMarker);

// add destination marker
                        Marker endMarker = new Marker(mapView);
                        endMarker.setPosition(destination);
                        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        endMarker.setTitle("Destination");
                        mapView.getOverlays().add(endMarker);

                        FirebaseDatabase.getInstance()
                                .getReference("vehicle/totalKm")
                                .setValue(distanceKm);


                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startSpeedUpdates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) {

                        float speedM_S = loc.getSpeed();     // m/s
                        float speedKmH = speedM_S * 3.6f;    // convert to km/h

                        tvSpeedValue.setText(String.format(Locale.US, "%.1f km/h", speedKmH));
                    }
                }
        );
    }



}

package com.example.trackify;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class BestPathActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private TextView tvSpeedValue, tvDistanceValue;
    private OkHttpClient client = new OkHttpClient();

    // origin حنحدثها بأول موقع يجي من الـ GPS
    private GeoPoint origin = null;

    // الوجهة ثابتة (غيّريها على كيفك)-------------------------
    private GeoPoint destination = new GeoPoint(24.0889, 38.0644);

    private Polyline routePolyline;
    private Marker userMarker;

    private LocationManager locationManager;

    // عشان نعرف هل رسمنا المسار ولا لسه
    private boolean routeInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تهيئة إعدادات OSMDroid
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_best_path);

        // ربط عناصر الواجهة
        mapView = findViewById(R.id.osmMapView);   // تأكدي من الـ id في XML
        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        tvDistanceValue = findViewById(R.id.tvDistanceValue);

        // قيم مبدئية (front-end)
        tvSpeedValue.setText("0 km/h");
        tvDistanceValue.setText("-- km");

        // إعداد الخريطة
        setupMap();

        // ما نرسم مسار هنا! ننتظر أول GPS location
        startLocationUpdates();
    }

    // إعداد الخريطة
    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(11.0);

        // مبدئياً نوسّط على السعودية ككل مثلاً
        mapController.setCenter(new GeoPoint(23.8859, 45.0792));
    }

    // استدعاء OSRM للحصول على أفضل مسار بين origin و destination
    private void fetchRouteFromOsrm() {
        if (origin == null) {
            Log.w(TAG, "fetchRouteFromOsrm: origin is null, skipping");
            return;
        }

        // OSRM يستخدم (lon,lat)
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + origin.getLongitude() + "," + origin.getLatitude() + ";"
                + destination.getLongitude() + "," + destination.getLatitude()
                + "?overview=full&geometries=geojson";

        Log.d(TAG, "OSRM URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "OSRM call failed", e);
                runOnUiThread(() ->
                        Toast.makeText(BestPathActivity.this,
                                "Failed to call OSRM API", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unsuccessful OSRM response: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(BestPathActivity.this,
                                    "OSRM error code: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                String responseBody = response.body().string();
                Log.d(TAG, "OSRM response: " + responseBody);

                try {
                    JSONObject json = new JSONObject(responseBody);

                    String code = json.getString("code");
                    if (!"Ok".equalsIgnoreCase(code)) {
                        Log.e(TAG, "OSRM status is not Ok: " + code);
                        runOnUiThread(() ->
                                Toast.makeText(BestPathActivity.this,
                                        "OSRM status: " + code, Toast.LENGTH_LONG).show());
                        return;
                    }

                    JSONArray routes = json.getJSONArray("routes");
                    if (routes.length() == 0) {
                        runOnUiThread(() ->
                                Toast.makeText(BestPathActivity.this,
                                        "No routes found", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    JSONObject route = routes.getJSONObject(0);

                    // المسافة الكلية للمسار (متر)
                    double distanceMeters = route.getDouble("distance");
                    final double distanceKm = distanceMeters / 1000.0;

                    // شكل المسار (GeoJSON)
                    JSONObject geometry = route.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    List<GeoPoint> path = new ArrayList<>();
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray coord = coordinates.getJSONArray(i);
                        double lon = coord.getDouble(0);
                        double lat = coord.getDouble(1);
                        path.add(new GeoPoint(lat, lon));
                    }

                    runOnUiThread(() -> {
                        // إزالة مسار قديم إن وجد
                        if (routePolyline != null) {
                            mapView.getOverlayManager().remove(routePolyline);
                        }

                        // رسم المسار
                        routePolyline = new Polyline();
                        routePolyline.setPoints(path);
                        routePolyline.setWidth(8f);
                        routePolyline.setColor(0xFF2196F3); // أزرق

                        mapView.getOverlayManager().add(routePolyline);
                        mapView.invalidate();

                        // تحديث المسافة المتبقية
                        tvDistanceValue.setText(
                                String.format(Locale.getDefault(), "%.1f km", distanceKm)
                        );
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error", e);
                    runOnUiThread(() ->
                            Toast.makeText(BestPathActivity.this,
                                    "Parsing OSRM response failed", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // 🔹 بدء طلب صلاحية الموقع وتشغيل التتبع
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            beginLocationUpdates();
        }
    }

    // فعلياً نطلب التحديثات من الـ GPS
    private void beginLocationUpdates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, "LocationManager is null", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                3000L,
                5f,
                locationListener
        );
    }

    // الـ Listener اللي يستقبل موقع المستخدم من الـ GPS
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            GeoPoint current = new GeoPoint(lat, lon);
            Log.d(TAG, "User location: " + lat + ", " + lon);

            // أول مرة فقط:
            if (!routeInitialized) {
                origin = current;          // خلي نقطة البداية = موقع المستخدم الحقيقي
                fetchRouteFromOsrm();      // ارسم المسار من هنا للوجهة
                routeInitialized = true;
            }

            // حرّكي ماركر السيارة دائماً مع الموقع
            updateUserPosition(current);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) { }

        @Override
        public void onProviderDisabled(@NonNull String provider) { }
    };

    // 🔹 تحريك ماركر المستخدم (بدون تغيير origin بعد أول مرة)
    private void updateUserPosition(GeoPoint current) {

        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setPosition(current);

            // ماركر افتراضي (أزرق)
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            mapView.getOverlays().add(userMarker);
        } else {
            userMarker.setPosition(current);
        }

        mapView.getController().animateTo(current);
        mapView.invalidate();
    }


    // نتيجة طلب الصلاحيات
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                beginLocationUpdates();
            } else {
                Toast.makeText(this,
                        "Location permission is required to track user.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // مهم لـ OSMDroid + إيقاف التتبع
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        super.onPause();
    }
}

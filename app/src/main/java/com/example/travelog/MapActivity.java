package com.example.travelog;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.travelog.databinding.ActivityMapBinding;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class MapActivity extends AppCompatActivity {

    private ActivityMapBinding binding;
    private MapView mapView;
    private final Map<Marker, Memory> markerMemoryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid configuration — must happen before MapView is used
        Configuration.getInstance().setUserAgentValue(getPackageName());

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the osmdroid MapView
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK); // OpenStreetMap tiles
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(5.0);
        // Default center: Turkey
        mapView.getController().setCenter(new GeoPoint(39.0, 35.0));

        // Floating back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Bottom navigation
        binding.bottomNav.setSelectedItemId(R.id.nav_map);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, StatsActivity.class));
                return true;
            }
            return true; // nav_map — already here
        });

        loadMarkersAsync();
    }

    // osmdroid requires lifecycle forwarding
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    // ── Load memories and draw markers + route ─────────────────────────────

    private void loadMarkersAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Sorted ASC (old → new) for route drawing
            List<Memory> memories = AppDatabase.getInstance(this).memoryDao().getAllMemories();
            Collections.reverse(memories);

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            List<GeoPoint> routePoints = new ArrayList<>();
            List<Object[]> pendingMarkers = new ArrayList<>(); // {GeoPoint, Memory}

            double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
            boolean hasMarker = false;

            for (Memory memory : memories) {
                try {
                    List<Address> addresses = geocoder.getFromLocationName(memory.city, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        double lat = addresses.get(0).getLatitude();
                        double lng = addresses.get(0).getLongitude();
                        GeoPoint position = new GeoPoint(lat, lng);

                        routePoints.add(position);
                        pendingMarkers.add(new Object[]{position, memory});

                        if (lat < minLat) minLat = lat;
                        if (lat > maxLat) maxLat = lat;
                        if (lng < minLng) minLng = lng;
                        if (lng > maxLng) maxLng = lng;

                        hasMarker = true;
                    }
                } catch (IOException ignored) {
                    // Geocoding failed for this city — skip
                }
            }

            final boolean hasMrk = hasMarker;
            final List<GeoPoint> route = new ArrayList<>(routePoints);
            final List<Object[]> markers = new ArrayList<>(pendingMarkers);
            final double fMinLat = minLat, fMaxLat = maxLat;
            final double fMinLng = minLng, fMaxLng = maxLng;

            runOnUiThread(() -> {
                if (!hasMrk) {
                    Toast.makeText(this, "Haritada gösterilecek anı bulunamadı",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Draw polyline first (so it appears below markers)
                if (route.size() > 1) {
                    Polyline polyline = new Polyline();
                    polyline.setPoints(route);
                    polyline.getOutlinePaint().setColor(Color.argb(140, 0, 80, 203)); // #0050CB 55% opacity
                    polyline.getOutlinePaint().setStrokeWidth(10f);
                    mapView.getOverlays().add(0, polyline);
                }

                // Add markers on top
                for (Object[] entry : markers) {
                    addMarker((GeoPoint) entry[0], (Memory) entry[1]);
                }

                // Zoom/pan to fit all markers
                if (route.size() == 1) {
                    mapView.getController().setZoom(10.0);
                    mapView.getController().setCenter(route.get(0));
                } else {
                    // Add 10% padding around the bounding box
                    double latPad = Math.max((fMaxLat - fMinLat) * 0.15, 0.5);
                    double lngPad = Math.max((fMaxLng - fMinLng) * 0.15, 0.5);
                    BoundingBox box = new BoundingBox(
                            fMaxLat + latPad, fMaxLng + lngPad,
                            fMinLat - latPad, fMinLng - lngPad);
                    mapView.zoomToBoundingBox(box, true, 100);
                }

                mapView.invalidate();
            });
        });
    }

    /** Create and place a single osmdroid Marker for the given Memory. */
    private void addMarker(GeoPoint position, Memory memory) {
        Marker marker = new Marker(mapView);
        marker.setPosition(position);
        marker.setTitle(memory.title);
        marker.setSnippet("📍 " + memory.city + "  📅 " + memory.date);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Tint the map pin based on memory type
        int tintColor;
        if (memory.isFuturePlan)    tintColor = 0xFFFF8F00; // amber  — plan
        else if (memory.isFavorite) tintColor = 0xFFF44336; // red    — favourite
        else                        tintColor = 0xFF0050CB; // blue   — normal

        Drawable icon = AppCompatResources.getDrawable(this, R.drawable.ic_map);
        if (icon != null) {
            icon = DrawableCompat.wrap(icon.mutate());
            DrawableCompat.setTint(icon, tintColor);
        }
        marker.setIcon(icon);

        // Tap once → show info bubble; tap again (or tap bubble) → open detail
        marker.setOnMarkerClickListener((m, mapV) -> {
            if (m.isInfoWindowShown()) {
                Memory mem = markerMemoryMap.get(m);
                if (mem != null) {
                    Intent intent = new Intent(MapActivity.this, DetailActivity.class);
                    intent.putExtra("memory", mem);
                    startActivity(intent);
                }
            } else {
                m.showInfoWindow();
            }
            return true;
        });

        markerMemoryMap.put(marker, memory);
        mapView.getOverlays().add(marker);
    }
}

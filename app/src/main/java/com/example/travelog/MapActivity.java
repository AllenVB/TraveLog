package com.example.travelog;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelog.databinding.ActivityMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMapBinding binding;
    private GoogleMap googleMap;
    private final Map<Marker, Memory> markerMemoryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@androidx.annotation.NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Marker tıklanınca anı detayını aç
        googleMap.setOnInfoWindowClickListener(marker -> {
            Memory memory = markerMemoryMap.get(marker);
            if (memory != null) {
                Intent intent = new Intent(MapActivity.this, DetailActivity.class);
                intent.putExtra("memory", memory);
                startActivity(intent);
            }
        });

        loadMarkersAsync();
    }

    private void loadMarkersAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Tarihe göre sıralı — rota çizimi için önemli
            List<Memory> memories = AppDatabase.getInstance(this).memoryDao().getAllMemories();
            // DB DESC sırası → ters çevir → ASC (eskiden yeniye) rota için
            java.util.Collections.reverse(memories);

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            List<LatLng> routePoints = new ArrayList<>();
            boolean hasMarker = false;

            for (Memory memory : memories) {
                try {
                    List<Address> addresses = geocoder.getFromLocationName(memory.city, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        LatLng position = new LatLng(
                                addresses.get(0).getLatitude(),
                                addresses.get(0).getLongitude());

                        routePoints.add(position);
                        boundsBuilder.include(position);
                        hasMarker = true;

                        final boolean isFav     = memory.isFavorite;
                        final boolean isPlan    = memory.isFuturePlan;
                        final LatLng finalPos   = position;
                        final Memory finalMemory = memory;

                        runOnUiThread(() -> {
                            float hue = isPlan
                                    ? BitmapDescriptorFactory.HUE_YELLOW
                                    : (isFav ? BitmapDescriptorFactory.HUE_RED
                                             : BitmapDescriptorFactory.HUE_AZURE);
                            MarkerOptions opts = new MarkerOptions()
                                    .position(finalPos)
                                    .title(finalMemory.title)
                                    .snippet("📍 " + finalMemory.city + "  📅 " + finalMemory.date)
                                    .icon(BitmapDescriptorFactory.defaultMarker(hue));
                            Marker marker = googleMap.addMarker(opts);
                            if (marker != null) markerMemoryMap.put(marker, finalMemory);
                        });
                    }
                } catch (IOException e) {
                    // Geocoding başarısız — atla
                }
            }

            final boolean hasMrk = hasMarker;
            final List<LatLng> route = new ArrayList<>(routePoints);

            runOnUiThread(() -> {
                if (!hasMrk) {
                    Toast.makeText(this, "Haritada gösterilecek anı bulunamadı",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // Rota polyline'ı çiz
                if (route.size() > 1) {
                    googleMap.addPolyline(new PolylineOptions()
                            .addAll(route)
                            .color(0x801565C0) // yarı saydam mavi
                            .width(5f));
                }
                LatLngBounds bounds = boundsBuilder.build();
                try {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                } catch (Exception ignored) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 4f));
                }
            });
        });
    }
}

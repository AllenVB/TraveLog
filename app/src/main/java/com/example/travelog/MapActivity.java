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

import java.io.IOException;
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
            List<Memory> memories = AppDatabase.getInstance(this).memoryDao().getAllMemories();

            // Her anı için şehri geocode et
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boolean hasMarker = false;

            for (Memory memory : memories) {
                try {
                    List<Address> addresses = geocoder.getFromLocationName(memory.city, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        double lat = addresses.get(0).getLatitude();
                        double lng = addresses.get(0).getLongitude();
                        LatLng position = new LatLng(lat, lng);

                        final boolean isFav = memory.isFavorite;
                        final LatLng finalPos = position;
                        final Memory finalMemory = memory;

                        runOnUiThread(() -> {
                            MarkerOptions options = new MarkerOptions()
                                    .position(finalPos)
                                    .title(finalMemory.title)
                                    .snippet("📍 " + finalMemory.city + "  📅 " + finalMemory.date)
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            isFav ? BitmapDescriptorFactory.HUE_RED
                                                  : BitmapDescriptorFactory.HUE_AZURE));

                            Marker marker = googleMap.addMarker(options);
                            if (marker != null) markerMemoryMap.put(marker, finalMemory);
                        });

                        boundsBuilder.include(position);
                        hasMarker = true;
                    }
                } catch (IOException e) {
                    // Geocoding başarısız — atla
                }
            }

            final boolean finalHasMarker = hasMarker;
            if (finalHasMarker) {
                final LatLngBounds bounds = boundsBuilder.build();
                runOnUiThread(() -> {
                    try {
                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 150));
                    } catch (Exception ignored) {
                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 4f));
                    }
                });
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Haritada gösterilecek anı bulunamadı",
                                Toast.LENGTH_SHORT).show());
            }
        });
    }
}

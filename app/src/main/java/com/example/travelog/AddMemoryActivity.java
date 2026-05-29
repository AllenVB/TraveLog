package com.example.travelog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.travelog.databinding.ActivityAddMemoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMemoryActivity extends AppCompatActivity {

    // openweathermap.org — ücretsiz API key
    private static final String OWM_API_KEY = "f0ad2bd63ed07359e4d47ed692a0ba5c";

    /**
     * OpenTripMap API key (opentripmap.io — ücretsiz).
     * Boş bırakılırsa OpenTripMap atlanır ve doğrudan Wikipedia kaynağı kullanılır,
     * böylece özellik anahtar olmadan da çalışır.
     */
    private static final String OTM_API_KEY = "";

    private ActivityAddMemoryBinding binding;
    private String selectedImageUri = "";
    private String weatherInfo = "";

    /** Hava durumu yanıtından gelen koordinatlarla çekilen ünlü yerler */
    private final List<Place> fetchedPlaces = new ArrayList<>();

    // ── Galeri Launcher (Anı Fotoğrafı) ──────────────────────────────────────
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) { }
                        selectedImageUri = imageUri.toString();
                        Glide.with(this).load(imageUri).centerCrop().into(binding.imageViewSelected);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMemoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnSelectImage.setOnClickListener(v -> checkPermissionAndOpenGallery());

        binding.btnGetWeather.setOnClickListener(v -> {
            String city = binding.editTextCity.getText().toString().trim();
            if (!city.isEmpty()) {
                fetchWeatherAndPlaces(city);
            } else {
                Toast.makeText(this, "Lütfen önce şehir girin", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSave.setOnClickListener(v -> saveMemory());
    }

    // ── Galeri İzin ───────────────────────────────────────────────────────────

    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 100);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        galleryLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Galeri izni reddedildi", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Hava Durumu + Koordinat Çekimi ────────────────────────────────────────

    private void fetchWeatherAndPlaces(String city) {
        binding.textViewWeatherInfo.setText("🌤 Yükleniyor...");
        showPlacesLoading();

        WeatherService service = RetrofitClient.getRetrofitInstance().create(WeatherService.class);
        service.getCurrentWeather(city, OWM_API_KEY, "metric").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call,
                                   @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse body = response.body();

                    float temp    = body.main.temp;
                    String desc   = body.weather.get(0).description;
                    int humidity  = body.main.humidity;
                    weatherInfo   = String.format(Locale.getDefault(),
                            "%.1f°C, %s, nem: %%%d", temp, desc, humidity);
                    binding.textViewWeatherInfo.setText("🌤 " + weatherInfo);

                    // Koordinatlar varsa ünlü yerleri çek
                    if (body.coord != null) {
                        fetchTopPlaces(body.coord.lat, body.coord.lon);
                    } else {
                        showPlacesError("Yer bilgisi alınamadı");
                    }
                } else {
                    binding.textViewWeatherInfo.setText("Şehir bulunamadı");
                    showPlacesError("Şehir bulunamadı");
                    Toast.makeText(AddMemoryActivity.this,
                            "Şehir adını kontrol edin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                binding.textViewWeatherInfo.setText("Bağlantı hatası");
                showPlacesError("Bağlantı hatası");
                Toast.makeText(AddMemoryActivity.this,
                        "İnternet bağlantısını kontrol edin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Ünlü Yerler: OpenTripMap (öncelikli) → Wikipedia (yedek) ───────────────

    /** En fazla kaç ünlü yer gösterilecek */
    private static final int MAX_PLACES = 3;

    private void fetchTopPlaces(double lat, double lon) {
        // OpenTripMap key varsa popülerliğe göre sıralı kaynağı dene; yoksa Wikipedia'ya geç
        if (!TextUtils.isEmpty(OTM_API_KEY)) {
            fetchFromOpenTripMap(lat, lon);
        } else {
            fetchFromWikipedia(lat, lon);
        }
    }

    /** Birincil kaynak: OpenTripMap — popülerliğe (rate) göre sıralı turistik yerler */
    private void fetchFromOpenTripMap(double lat, double lon) {
        OpenTripMapService otm = RetrofitClient.getOpenTripMapInstance()
                .create(OpenTripMapService.class);

        // rate=3h → yalnızca yüksek puanlı/ünlü yerler; popülerliğe göre azalan sırada döner
        otm.getPlaces(20000, lon, lat, "3h", "interesting_places", 15, "json", OTM_API_KEY)
                .enqueue(new Callback<List<OtmPlace>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<OtmPlace>> call,
                                           @NonNull Response<List<OtmPlace>> response) {
                        fetchedPlaces.clear();

                        if (response.isSuccessful() && response.body() != null) {
                            for (OtmPlace p : response.body()) {
                                if (p != null && isGoodPlaceTitle(p.name)
                                        && fetchedPlaces.size() < MAX_PLACES) {
                                    fetchedPlaces.add(new Place(0, p.name.trim()));
                                }
                            }
                        }

                        // OTM boş döndüyse Wikipedia'ya düş
                        if (fetchedPlaces.isEmpty()) {
                            fetchFromWikipedia(lat, lon);
                        } else {
                            showPlacesResult();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<OtmPlace>> call,
                                          @NonNull Throwable t) {
                        // Hata → yedek kaynağa geç
                        fetchFromWikipedia(lat, lon);
                    }
                });
    }

    /** Yedek kaynak: Wikipedia Geosearch — API key gerektirmez */
    private void fetchFromWikipedia(double lat, double lon) {
        String coords = lat + "|" + lon;

        WikipediaService wikiService = RetrofitClient.getWikipediaInstance()
                .create(WikipediaService.class);

        // Daha geniş aday havuzu (limit=20) → filtreden geçenlerin ilk 3'ünü alırız
        wikiService.getPlacesNearby("query", "geosearch", 10000, coords, 20, 0, "json")
                .enqueue(new Callback<WikiGeoSearchResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WikiGeoSearchResponse> call,
                                           @NonNull Response<WikiGeoSearchResponse> response) {
                        fetchedPlaces.clear();

                        if (response.isSuccessful() && response.body() != null
                                && response.body().query != null
                                && response.body().query.geosearch != null) {

                            for (WikiGeoSearchResponse.GeoPlace gp :
                                    response.body().query.geosearch) {
                                if (isGoodPlaceTitle(gp.title)
                                        && fetchedPlaces.size() < MAX_PLACES) {
                                    fetchedPlaces.add(new Place(0, gp.title.trim()));
                                }
                            }
                        }

                        showPlacesResult();
                    }

                    @Override
                    public void onFailure(@NonNull Call<WikiGeoSearchResponse> call,
                                          @NonNull Throwable t) {
                        showPlacesError("Yer bilgisi yüklenemedi");
                    }
                });
    }

    /**
     * Gezilebilir/ünlü bir yer başlığı mı? Tarihî olaylar, listeler, sayım/seçim
     * kayıtları ve anlam ayrımı sayfaları gibi "yer olmayan" başlıkları eler.
     */
    private boolean isGoodPlaceTitle(String title) {
        if (TextUtils.isEmpty(title)) return false;

        String t = title.toLowerCase(Locale.ROOT).trim();

        // Yıl/sayı ile başlayan başlıklar genelde olaydır: "740 Constantinople earthquake"
        if (t.matches("^\\d{3,4}\\b.*")) return false;

        String[] blocked = {
                "earthquake", "deprem", "list of", "liste", "(disambiguation)",
                "census", "election", "massacre", "battle of", "siege of",
                "treaty of", "uprising", "revolt", "war ", "timeline"
        };
        for (String b : blocked) {
            if (t.contains(b)) return false;
        }
        return true;
    }

    // ── Yerler UI ─────────────────────────────────────────────────────────────

    private void showPlacesLoading() {
        binding.cardPlacesPreview.setVisibility(View.VISIBLE);
        binding.progressPlaces.setVisibility(View.VISIBLE);
        binding.tvPlacesStatus.setVisibility(View.VISIBLE);
        binding.tvPlacesStatus.setText("Ünlü yerler aranıyor...");
        binding.layoutPlacesList.removeAllViews();
    }

    private void showPlacesResult() {
        binding.progressPlaces.setVisibility(View.GONE);

        if (fetchedPlaces.isEmpty()) {
            binding.tvPlacesStatus.setVisibility(View.VISIBLE);
            binding.tvPlacesStatus.setText("Bu şehir için yer bilgisi bulunamadı");
            binding.layoutPlacesList.removeAllViews();
            return;
        }

        binding.tvPlacesStatus.setVisibility(View.GONE);
        binding.layoutPlacesList.removeAllViews();

        int i = 1;
        for (Place place : fetchedPlaces) {
            TextView tv = new TextView(this);
            tv.setText(i + ".  📍 " + place.name);
            tv.setTextSize(14f);
            tv.setTextColor(getColor(R.color.text_primary));
            tv.setPadding(0, 10, 0, 10);
            binding.layoutPlacesList.addView(tv);

            // İnce ayırıcı
            if (i < fetchedPlaces.size()) {
                View divider = new View(this);
                divider.setBackgroundColor(getColor(R.color.divider));
                android.widget.LinearLayout.LayoutParams lp =
                        new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(lp);
                binding.layoutPlacesList.addView(divider);
            }
            i++;
        }
    }

    private void showPlacesError(String msg) {
        binding.cardPlacesPreview.setVisibility(View.VISIBLE);
        binding.progressPlaces.setVisibility(View.GONE);
        binding.tvPlacesStatus.setVisibility(View.VISIBLE);
        binding.tvPlacesStatus.setText(msg);
        binding.layoutPlacesList.removeAllViews();
    }

    // ── Kaydetme ──────────────────────────────────────────────────────────────

    private void saveMemory() {
        String title       = binding.editTextTitle.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();
        String city        = binding.editTextCity.getText().toString().trim();
        String date        = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

        if (title.isEmpty()) {
            binding.editTextTitle.setError("Başlık gerekli");
            return;
        }
        if (description.isEmpty()) {
            binding.editTextDescription.setError("Açıklama gerekli");
            return;
        }
        if (city.isEmpty()) {
            binding.editTextCity.setError("Şehir gerekli");
            return;
        }
        if (selectedImageUri.isEmpty()) {
            Toast.makeText(this, "Lütfen bir fotoğraf seçin", Toast.LENGTH_SHORT).show();
            return;
        }

        Memory memory = new Memory(title, description, city, selectedImageUri, weatherInfo, date);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // insert() artık long döndürüyor → place'lere memoryId atamak için kullanıyoruz
            long memoryId = AppDatabase.getInstance(this).memoryDao().insert(memory);

            if (!fetchedPlaces.isEmpty()) {
                for (Place place : fetchedPlaces) {
                    place.memoryId = (int) memoryId;
                }
                AppDatabase.getInstance(this).placeDao().insertAll(fetchedPlaces);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Anı kaydedildi! ✈", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}

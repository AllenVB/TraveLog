package com.example.travelog;

import android.Manifest;
import android.app.DatePickerDialog;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMemoryActivity extends AppCompatActivity {

    private static final String OWM_API_KEY = "f0ad2bd63ed07359e4d47ed692a0ba5c";
    private static final String OTM_API_KEY = "";
    private static final int MAX_PLACES = 3;

    private ActivityAddMemoryBinding binding;
    private String selectedImageUri = "";
    private String weatherInfo = "";
    private String selectedDate = "";
    private boolean isFuturePlan = false;

    /** Düzenleme modu — null ise yeni anı, doluysa var olan anı */
    private Memory editMemory = null;

    private final List<Place> fetchedPlaces = new ArrayList<>();

    // ── Galeri Launcher ───────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {}
                        selectedImageUri = uri.toString();
                        Glide.with(this).load(uri).centerCrop().into(binding.imageViewSelected);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMemoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Düzenleme modu?
        editMemory = (Memory) getIntent().getSerializableExtra("editMemory");
        if (editMemory != null) {
            binding.toolbar.setTitle("Anıyı Düzenle");
            binding.btnSave.setText("💾  Güncelle");
            prefillForEdit(editMemory);
        }

        binding.btnSelectImage.setOnClickListener(v -> checkPermissionAndOpenGallery());
        binding.btnGetWeather.setOnClickListener(v -> {
            String city = binding.editTextCity.getText().toString().trim();
            if (!city.isEmpty()) fetchWeatherAndPlaces(city);
            else Toast.makeText(this, "Lütfen önce şehir girin", Toast.LENGTH_SHORT).show();
        });
        binding.switchFuturePlan.setOnCheckedChangeListener((btn, checked) -> {
            isFuturePlan = checked;
            binding.layoutDatePicker.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnSave.setOnClickListener(v -> saveMemory());
    }

    // ── Düzenleme modu: alanları doldur ──────────────────────────────────────

    private void prefillForEdit(Memory m) {
        binding.editTextTitle.setText(m.title);
        binding.editTextDescription.setText(m.description);
        binding.editTextCity.setText(m.city);
        binding.editTextCountry.setText(m.country);
        selectedImageUri = m.imageUri != null ? m.imageUri : "";
        weatherInfo = m.weather != null ? m.weather : "";
        if (!weatherInfo.isEmpty())
            binding.textViewWeatherInfo.setText("🌤 " + weatherInfo);
        if (!selectedImageUri.isEmpty())
            Glide.with(this).load(Uri.parse(selectedImageUri)).centerCrop()
                    .into(binding.imageViewSelected);
        isFuturePlan = m.isFuturePlan;
        binding.switchFuturePlan.setChecked(isFuturePlan);
        if (isFuturePlan) {
            selectedDate = m.date;
            binding.layoutDatePicker.setVisibility(View.VISIBLE);
            binding.tvSelectedDate.setText(m.date);
        }
    }

    // ── Tarih Seçici ──────────────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d.%02d.%04d", day, month + 1, year);
            binding.tvSelectedDate.setText(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    // ── Galeri izni ───────────────────────────────────────────────────────────

    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            openGallery();
        else
            ActivityCompat.requestPermissions(this, new String[]{permission}, 100);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        galleryLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) openGallery();
        else Toast.makeText(this, "Galeri izni reddedildi", Toast.LENGTH_SHORT).show();
    }

    // ── Hava Durumu + Koordinat ────────────────────────────────────────────────

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
                    weatherInfo = String.format(Locale.getDefault(), "%.1f°C, %s, nem: %%%d",
                            body.main.temp, body.weather.get(0).description, body.main.humidity);
                    binding.textViewWeatherInfo.setText("🌤 " + weatherInfo);
                    if (body.coord != null) fetchTopPlaces(body.coord.lat, body.coord.lon);
                    else showPlacesError("Yer bilgisi alınamadı");
                } else {
                    binding.textViewWeatherInfo.setText("Şehir bulunamadı");
                    showPlacesError("Şehir bulunamadı");
                    Toast.makeText(AddMemoryActivity.this, "Şehir adını kontrol edin", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                binding.textViewWeatherInfo.setText("Bağlantı hatası");
                showPlacesError("Bağlantı hatası");
            }
        });
    }

    // ── OpenStreetMap Overpass (öncelikli) → Wikipedia (yedek) ────────────────

    private void fetchTopPlaces(double lat, double lon) {
        fetchFromOverpass(lat, lon);
    }

    /**
     * Gerçek turistik/tarihi yerleri OSM etiketlerinden çeker.
     * Wikipedia/Wikidata kaydı olan (daha ünlü) yerler öne alınır.
     */
    private void fetchFromOverpass(double lat, double lon) {
        String q = String.format(Locale.US,
                "[out:json][timeout:25];(" +
                "node[\"tourism\"=\"attraction\"](around:25000,%1$f,%2$f);" +
                "way[\"tourism\"=\"attraction\"](around:25000,%1$f,%2$f);" +
                "node[\"historic\"~\"monument|memorial|castle|archaeological_site|ruins|fort|palace|tomb\"](around:25000,%1$f,%2$f);" +
                "way[\"historic\"~\"monument|memorial|castle|archaeological_site|ruins|fort|palace|tomb\"](around:25000,%1$f,%2$f);" +
                "node[\"tourism\"=\"museum\"](around:25000,%1$f,%2$f);" +
                "way[\"tourism\"=\"museum\"](around:25000,%1$f,%2$f);" +
                ");out center 120;",
                lat, lon);

        OverpassService overpass = RetrofitClient.getOverpassInstance().create(OverpassService.class);
        overpass.query(q).enqueue(new Callback<OverpassResponse>() {
            @Override
            public void onResponse(@NonNull Call<OverpassResponse> call,
                                   @NonNull Response<OverpassResponse> response) {
                fetchedPlaces.clear();
                if (response.isSuccessful() && response.body() != null
                        && response.body().elements != null) {

                    List<OverpassResponse.Element> els = response.body().elements;
                    // Ünlü (wiki kaydı olan) yerler önce, sonra kategori önceliği
                    els.sort((a, b) -> rank(b) - rank(a));

                    java.util.Set<String> seen = new java.util.HashSet<>();
                    for (OverpassResponse.Element e : els) {
                        if (e.tags == null) continue;
                        String name = e.tags.nameTr != null ? e.tags.nameTr
                                : (e.tags.name != null ? e.tags.name : e.tags.nameEn);
                        if (!isGoodPlaceTitle(name)) continue;
                        name = name.trim();
                        String key = name.toLowerCase(Locale.ROOT);
                        if (!seen.add(key)) continue;
                        fetchedPlaces.add(new Place(0, name));
                        if (fetchedPlaces.size() >= MAX_PLACES) break;
                    }
                }
                if (fetchedPlaces.isEmpty()) fetchFromWikipedia(lat, lon);
                else showPlacesResult();
            }
            @Override
            public void onFailure(@NonNull Call<OverpassResponse> call, @NonNull Throwable t) {
                fetchFromWikipedia(lat, lon);
            }
        });
    }

    /** Yer puanı: wiki kaydı + kategori önemi (yüksek = daha öne). */
    private int rank(OverpassResponse.Element e) {
        if (e.tags == null) return 0;
        int score = 0;
        if (e.tags.wikidata != null || e.tags.wikipedia != null) score += 100;
        if ("attraction".equals(e.tags.tourism)) score += 30;
        if (e.tags.historic != null) score += 20;
        if ("museum".equals(e.tags.tourism)) score += 10;
        return score;
    }

    private void fetchFromOpenTripMap(double lat, double lon) {
        OpenTripMapService otm = RetrofitClient.getOpenTripMapInstance().create(OpenTripMapService.class);
        otm.getPlaces(20000, lon, lat, "3h", "interesting_places", 15, "json", OTM_API_KEY)
                .enqueue(new Callback<List<OtmPlace>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<OtmPlace>> call,
                                           @NonNull Response<List<OtmPlace>> response) {
                        fetchedPlaces.clear();
                        if (response.isSuccessful() && response.body() != null)
                            for (OtmPlace p : response.body())
                                if (p != null && isGoodPlaceTitle(p.name) && fetchedPlaces.size() < MAX_PLACES)
                                    fetchedPlaces.add(new Place(0, p.name.trim()));
                        if (fetchedPlaces.isEmpty()) fetchFromWikipedia(lat, lon);
                        else showPlacesResult();
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<OtmPlace>> call, @NonNull Throwable t) {
                        fetchFromWikipedia(lat, lon);
                    }
                });
    }

    private void fetchFromWikipedia(double lat, double lon) {
        String coords = lat + "|" + lon;
        WikipediaService wiki = RetrofitClient.getWikipediaInstance().create(WikipediaService.class);
        wiki.getPlacesNearby("query", "geosearch", 10000, coords, 20, 0, "json")
                .enqueue(new Callback<WikiGeoSearchResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WikiGeoSearchResponse> call,
                                           @NonNull Response<WikiGeoSearchResponse> response) {
                        fetchedPlaces.clear();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().query != null
                                && response.body().query.geosearch != null)
                            for (WikiGeoSearchResponse.GeoPlace gp : response.body().query.geosearch)
                                if (isGoodPlaceTitle(gp.title) && fetchedPlaces.size() < MAX_PLACES)
                                    fetchedPlaces.add(new Place(0, gp.title.trim()));
                        showPlacesResult();
                    }
                    @Override
                    public void onFailure(@NonNull Call<WikiGeoSearchResponse> call, @NonNull Throwable t) {
                        showPlacesError("Yer bilgisi yüklenemedi");
                    }
                });
    }

    private boolean isGoodPlaceTitle(String title) {
        if (TextUtils.isEmpty(title)) return false;
        String t = title.toLowerCase(Locale.ROOT).trim();
        if (t.matches("^\\d{3,4}\\b.*")) return false;
        for (String b : new String[]{"earthquake","deprem","list of","liste","(disambiguation)",
                "census","election","massacre","battle of","siege of","treaty of",
                "uprising","revolt","war ","timeline"})
            if (t.contains(b)) return false;
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

    // ── Kaydet / Güncelle ─────────────────────────────────────────────────────

    private void saveMemory() {
        String title       = binding.editTextTitle.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();
        String city        = binding.editTextCity.getText().toString().trim();
        String country     = binding.editTextCountry.getText().toString().trim();
        String date;

        if (isFuturePlan && !TextUtils.isEmpty(selectedDate)) date = selectedDate;
        else if (editMemory != null) date = editMemory.date;
        else date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

        if (title.isEmpty())       { binding.editTextTitle.setError("Başlık gerekli"); return; }
        if (description.isEmpty()) { binding.editTextDescription.setError("Açıklama gerekli"); return; }
        if (city.isEmpty())        { binding.editTextCity.setError("Şehir gerekli"); return; }
        if (selectedImageUri.isEmpty()) {
            Toast.makeText(this, "Lütfen bir fotoğraf seçin", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            if (editMemory != null) {
                // Güncelleme modu
                editMemory.title       = title;
                editMemory.description = description;
                editMemory.city        = city;
                editMemory.country     = country;
                editMemory.imageUri    = selectedImageUri;
                editMemory.weather     = weatherInfo;
                editMemory.date        = date;
                editMemory.isFuturePlan = isFuturePlan;
                db.memoryDao().update(editMemory);

                // Yerler güncelle
                if (!fetchedPlaces.isEmpty()) {
                    db.placeDao().deleteByMemoryId(editMemory.id);
                    for (Place place : fetchedPlaces) place.memoryId = editMemory.id;
                    db.placeDao().insertAll(fetchedPlaces);
                }
            } else {
                // Yeni anı
                Memory memory = new Memory(title, description, city, selectedImageUri, weatherInfo, date);
                memory.country = country;
                memory.isFuturePlan = isFuturePlan;
                long memoryId = db.memoryDao().insert(memory);
                if (!fetchedPlaces.isEmpty()) {
                    for (Place place : fetchedPlaces) place.memoryId = (int) memoryId;
                    db.placeDao().insertAll(fetchedPlaces);
                }
            }

            runOnUiThread(() -> {
                Toast.makeText(this,
                        editMemory != null ? "Anı güncellendi ✅" : "Anı kaydedildi! ✈",
                        Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}

package com.example.travelog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMemoryActivity extends AppCompatActivity {

    // openweathermap.org adresinden ücretsiz API key alın
    private static final String API_KEY = "f0ad2bd63ed07359e4d47ed692a0ba5c";

    private ActivityAddMemoryBinding binding;
    private String selectedImageUri = "";
    private String weatherInfo = "";

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
                fetchWeather(city);
            } else {
                Toast.makeText(this, "Lütfen önce şehir girin", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSave.setOnClickListener(v -> saveMemory());
    }

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

    private void fetchWeather(String city) {
        binding.textViewWeatherInfo.setText("🌤 Yükleniyor...");

        WeatherService service = RetrofitClient.getRetrofitInstance().create(WeatherService.class);
        service.getCurrentWeather(city, API_KEY, "metric").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call,
                                   @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    float temp = response.body().main.temp;
                    String desc = response.body().weather.get(0).description;
                    int humidity = response.body().main.humidity;
                    weatherInfo = String.format(Locale.getDefault(),
                            "%.1f°C, %s, nem: %%%d", temp, desc, humidity);
                    binding.textViewWeatherInfo.setText("🌤 " + weatherInfo);
                } else {
                    binding.textViewWeatherInfo.setText("Şehir bulunamadı");
                    Toast.makeText(AddMemoryActivity.this,
                            "Şehir adını kontrol edin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                binding.textViewWeatherInfo.setText("Bağlantı hatası");
                Toast.makeText(AddMemoryActivity.this,
                        "İnternet bağlantısını kontrol edin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveMemory() {
        String title = binding.editTextTitle.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();
        String city = binding.editTextCity.getText().toString().trim();
        String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

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
            AppDatabase.getInstance(this).memoryDao().insert(memory);
            runOnUiThread(() -> {
                Toast.makeText(this, "Anı kaydedildi! ✈", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}

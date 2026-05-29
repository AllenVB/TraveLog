package com.example.travelog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.travelog.databinding.ActivityDetailBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity implements PlaceAdapter.Listener {

    private ActivityDetailBinding binding;
    private Memory memory;
    private MenuItem menuFavorite;

    // ── Ünlü Yerler ──────────────────────────────────────────────────────────
    private PlaceAdapter placeAdapter;
    private final List<Place> placeList = new ArrayList<>();

    /** Fotoğraf seçimi bekleyen yer indeksi */
    private int pendingPhotoPosition = -1;

    // ── Galeri Launcher (Yer Fotoğrafı) ──────────────────────────────────────
    private final ActivityResultLauncher<Intent> placePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && pendingPhotoPosition >= 0) {

                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) { }

                        String uriStr = uri.toString();
                        placeAdapter.updatePlacePhoto(pendingPhotoPosition, uriStr);

                        // DB'yi güncelle
                        Place place = placeAdapter.getPlace(pendingPhotoPosition);
                        place.photoUri = uriStr;
                        Executors.newSingleThreadExecutor().execute(() ->
                                AppDatabase.getInstance(this).placeDao().update(place));
                    }
                }
                pendingPhotoPosition = -1;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        memory = (Memory) getIntent().getSerializableExtra("memory");

        if (memory != null) {
            bindMemory();
            setupPlacesRecyclerView();
            loadPlaces();
        }
    }

    // ── Anı Bilgileri ─────────────────────────────────────────────────────────

    private void bindMemory() {
        binding.toolbar.setTitle(memory.title);
        binding.textViewTitleDetail.setText(memory.title);
        binding.textViewCityDetail.setText("📍 " + memory.city);
        binding.textViewDateDetail.setText("📅 " + memory.date);
        binding.textViewDescriptionDetail.setText(memory.description);
        binding.textViewWeatherDetail.setText(
                TextUtils.isEmpty(memory.weather)
                        ? "🌤 Hava bilgisi yok"
                        : "🌤 " + memory.weather);

        if (!TextUtils.isEmpty(memory.imageUri)) {
            Glide.with(this)
                    .load(Uri.parse(memory.imageUri))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(android.R.color.darker_gray)
                    .into(binding.imageViewDetail);
        }
    }

    // ── Ünlü Yerler RecyclerView ──────────────────────────────────────────────

    private void setupPlacesRecyclerView() {
        placeAdapter = new PlaceAdapter(placeList, this);
        binding.recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewPlaces.setAdapter(placeAdapter);
        binding.recyclerViewPlaces.setNestedScrollingEnabled(false);
    }

    private void loadPlaces() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Place> loaded = AppDatabase.getInstance(this)
                    .placeDao()
                    .getPlacesForMemory(memory.id);

            runOnUiThread(() -> {
                if (!loaded.isEmpty()) {
                    placeList.addAll(loaded);
                    placeAdapter.notifyDataSetChanged();

                    // Yerler bölümünü görünür yap
                    binding.dividerPlaces.setVisibility(View.VISIBLE);
                    binding.tvPlacesHeader.setVisibility(View.VISIBLE);
                    binding.recyclerViewPlaces.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    // ── PlaceAdapter.Listener ─────────────────────────────────────────────────

    @Override
    public void onVisitedToggled(int position, boolean isVisited) {
        Place place = placeAdapter.getPlace(position);
        place.isVisited = isVisited;

        if (!isVisited) {
            place.photoUri = null;
        }

        Executors.newSingleThreadExecutor().execute(() ->
                AppDatabase.getInstance(this).placeDao().update(place));

        String msg = isVisited
                ? "✅ " + place.name + " gezildi!"
                : "Gezilmedi olarak işaretlendi";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPickPhoto(int position) {
        pendingPhotoPosition = position;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        placePhotoLauncher.launch(intent);
    }

    // ── Toolbar Menüsü ────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        menuFavorite = menu.findItem(R.id.action_favorite);
        updateFavoriteIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_favorite) {
            toggleFavorite();
            return true;
        } else if (id == R.id.action_share) {
            showShareOptions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Favori ────────────────────────────────────────────────────────────────

    private void toggleFavorite() {
        memory.isFavorite = !memory.isFavorite;
        updateFavoriteIcon();

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).memoryDao().update(memory);
            runOnUiThread(() -> {
                String msg = memory.isFavorite
                        ? "⭐ Favorilere eklendi"
                        : "Favorilerden çıkarıldı";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateFavoriteIcon() {
        if (menuFavorite == null) return;
        menuFavorite.setIcon(memory.isFavorite
                ? R.drawable.ic_favorite
                : R.drawable.ic_favorite_border);
    }

    // ── Paylaşma ──────────────────────────────────────────────────────────────

    private void showShareOptions() {
        String[] options = {"💬 WhatsApp'ta Paylaş", "📤 Diğer Uygulamalar"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Anıyı Paylaş")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) shareToWhatsApp();
                    else shareToOthers();
                })
                .show();
    }

    private String buildShareText() {
        StringBuilder sb = new StringBuilder();
        sb.append("✈ *").append(memory.title).append("*\n\n");
        sb.append("📍 Şehir: ").append(memory.city).append("\n");
        sb.append("📅 Tarih: ").append(memory.date).append("\n");
        if (!TextUtils.isEmpty(memory.weather))
            sb.append("🌤 Hava: ").append(memory.weather).append("\n");

        // Gezilen yerleri de paylaş
        if (!placeList.isEmpty()) {
            sb.append("\n🗺 Gezilen Yerler:\n");
            for (Place p : placeList) {
                sb.append(p.isVisited ? "✅ " : "◻ ").append(p.name).append("\n");
            }
        }

        sb.append("\n").append(memory.description).append("\n\n");
        sb.append("_TraveLog uygulamasından paylaşıldı_ ✈");
        return sb.toString();
    }

    private void shareToWhatsApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setPackage("com.whatsapp");

            if (!TextUtils.isEmpty(memory.imageUri)) {
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(memory.imageUri));
                intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            }
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp yüklü değil", Toast.LENGTH_SHORT).show();
            shareToOthers();
        }
    }

    private void shareToOthers() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        if (!TextUtils.isEmpty(memory.imageUri)) {
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(memory.imageUri));
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
        }
        startActivity(Intent.createChooser(intent, "Anıyı Paylaş"));
    }
}

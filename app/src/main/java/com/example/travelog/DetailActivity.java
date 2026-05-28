package com.example.travelog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.travelog.databinding.ActivityDetailBinding;

import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;
    private Memory memory;
    private MenuItem menuFavorite;

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
        }
    }

    private void bindMemory() {
        binding.toolbar.setTitle(memory.title);
        binding.textViewTitleDetail.setText(memory.title);
        binding.textViewCityDetail.setText("📍 " + memory.city);
        binding.textViewDateDetail.setText("📅 " + memory.date);
        binding.textViewDescriptionDetail.setText(memory.description);
        binding.textViewWeatherDetail.setText(
                TextUtils.isEmpty(memory.weather) ? "🌤 Hava bilgisi yok" : "🌤 " + memory.weather);

        if (!TextUtils.isEmpty(memory.imageUri)) {
            Glide.with(this)
                    .load(Uri.parse(memory.imageUri))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(android.R.color.darker_gray)
                    .into(binding.imageViewDetail);
        }
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
                String msg = memory.isFavorite ? "⭐ Favorilere eklendi" : "Favorilerden çıkarıldı";
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
        sb.append("\n").append(memory.description).append("\n\n");
        sb.append("_TraveLog uygulamasından paylaşıldı_ ✈");
        return sb.toString();
    }

    /** WhatsApp'a özel paylaşım */
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
            shareToOthers(); // fallback
        }
    }

    /** Genel paylaşım (tüm uygulamalar) */
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

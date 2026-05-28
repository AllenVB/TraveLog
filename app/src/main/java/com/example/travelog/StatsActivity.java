package com.example.travelog;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelog.databinding.ActivityStatsBinding;

import java.util.List;
import java.util.concurrent.Executors;

public class StatsActivity extends AppCompatActivity {

    private ActivityStatsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadStats();
    }

    private void loadStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            int totalCount   = db.memoryDao().getTotalCount();
            int cityCount    = db.memoryDao().getUniqueCityCount();
            int favCount     = db.memoryDao().getFavoriteCount();
            List<MemoryDao.CityCount> cityCounts = db.memoryDao().getCityCounts();

            String topCity = cityCounts.isEmpty() ? "-" : cityCounts.get(0).city;

            runOnUiThread(() -> {
                binding.tvTotalMemories.setText(String.valueOf(totalCount));
                binding.tvUniqueCities.setText(String.valueOf(cityCount));
                binding.tvFavorites.setText(String.valueOf(favCount));
                binding.tvTopCity.setText(topCity);

                buildCityChart(cityCounts, totalCount);
            });
        });
    }

    /** Şehir bazlı bar chart oluştur */
    private void buildCityChart(List<MemoryDao.CityCount> cityCounts, int total) {
        binding.layoutCityStats.removeAllViews();

        if (cityCounts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Henüz veri yok");
            empty.setPadding(8, 8, 8, 8);
            binding.layoutCityStats.addView(empty);
            return;
        }

        for (MemoryDao.CityCount cc : cityCounts) {
            // Satır container
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 12);
            row.setLayoutParams(rowParams);

            // Şehir adı + sayı
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvCity = new TextView(this);
            tvCity.setText("📍 " + cc.city);
            tvCity.setTextSize(14f);
            tvCity.setTextColor(getColor(R.color.text_primary));
            LinearLayout.LayoutParams cityParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvCity.setLayoutParams(cityParams);

            TextView tvCount = new TextView(this);
            tvCount.setText(cc.cnt + " anı");
            tvCount.setTextSize(12f);
            tvCount.setTextColor(getColor(R.color.text_secondary));

            header.addView(tvCity);
            header.addView(tvCount);

            // Progress bar
            ProgressBar progressBar = new ProgressBar(this, null,
                    android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(total);
            progressBar.setProgress(cc.cnt);
            progressBar.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 16);
            pbParams.setMargins(0, 4, 0, 0);
            progressBar.setLayoutParams(pbParams);

            row.addView(header);
            row.addView(progressBar);
            binding.layoutCityStats.addView(row);
        }
    }
}

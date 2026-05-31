package com.example.travelog;

import android.content.Intent;
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

        // Back button (floating header)
        binding.btnBack.setOnClickListener(v -> finish());

        // Bottom navigation
        binding.bottomNav.setSelectedItemId(R.id.nav_profile);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            }
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            }
            return true; // nav_profile — already here
        });

        loadStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep profile selected when returning to this screen
        binding.bottomNav.setSelectedItemId(R.id.nav_profile);
    }

    private void loadStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            int totalCount        = db.memoryDao().getTotalCount();
            int cityCount         = db.memoryDao().getUniqueCityCount();
            int favCount          = db.memoryDao().getFavoriteCount();
            int planCount         = db.memoryDao().getFuturePlanCount();
            List<Place> allPlaces = db.placeDao().getAllPlaces();
            int visitedPlaces = 0;
            for (Place p : allPlaces) if (p.isVisited) visitedPlaces++;
            List<MemoryDao.CityCount> cityCounts = db.memoryDao().getCityCounts();

            String topCity = cityCounts.isEmpty() ? "-" : cityCounts.get(0).city;
            final int finalVisited = visitedPlaces;
            final int finalTotal   = allPlaces.size();

            runOnUiThread(() -> {
                binding.tvTotalMemories.setText(String.valueOf(totalCount));
                binding.tvUniqueCities.setText(String.valueOf(cityCount));
                binding.tvFavorites.setText(String.valueOf(favCount));
                binding.tvTopCity.setText(topCity);
                binding.tvVisitedPlaces.setText(finalVisited + "/" + finalTotal);
                binding.tvFuturePlans.setText(String.valueOf(planCount));

                buildCityChart(cityCounts, totalCount);
            });
        });
    }

    /** Build city bar chart dynamically */
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
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 12);
            row.setLayoutParams(rowParams);

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

            ProgressBar progressBar = new ProgressBar(this, null,
                    android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(Math.max(total, 1));
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

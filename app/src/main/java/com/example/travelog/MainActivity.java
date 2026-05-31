package com.example.travelog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelog.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int SORT_NEWEST    = 0;
    private static final int SORT_OLDEST    = 1;
    private static final int SORT_CITY_AZ   = 2;
    private static final int SORT_FAV_FIRST = 3;

    private static final int MODE_ALL   = 0;
    private static final int MODE_FAV   = 1;
    private static final int MODE_PLANS = 2;

    private ActivityMainBinding binding;
    private MemoryAdapter adapter;
    private List<Memory> allMemories = new ArrayList<>();
    private AppDatabase db;

    private int currentSortMode = SORT_NEWEST;
    private int filterMode = MODE_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // No action bar — using custom floating header

        db = AppDatabase.getInstance(this);

        adapter = new MemoryAdapter(allMemories, memory -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("memory", memory);
            startActivity(intent);
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();
        setupFilterChips();
        setupSwipeToDelete();
        setupNotifications();
        setupBottomNav();

        // Sort button (replaces menu item)
        binding.btnSort.setOnClickListener(v -> showSortDialog());

        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddMemoryActivity.class)));
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────

    private void setupBottomNav() {
        // Mark Home as selected
        binding.bottomNav.setSelectedItemId(R.id.nav_home);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, StatsActivity.class));
                return true;
            }
            // nav_home: already here
            return true;
        });
    }

    // ── Notification setup ─────────────────────────────────────────────────

    private void setupNotifications() {
        NotificationHelper.createChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            }
        }
        NotificationHelper.scheduleDailyAlarm(this);
    }

    // ── Sort Dialog ────────────────────────────────────────────────────────

    private void showSortDialog() {
        String[] options = {"En Yeni", "En Eski", "Şehir A-Z", "Favoriler Önce"};
        new AlertDialog.Builder(this)
                .setTitle("Sıralama")
                .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                    currentSortMode = which;
                    applyDisplayList();
                    dialog.dismiss();
                })
                .show();
    }

    // ── Search ─────────────────────────────────────────────────────────────

    private void setupSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updateEmptyState();
            }
        });
    }

    // ── Filter Chips ───────────────────────────────────────────────────────

    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipFavorites))  filterMode = MODE_FAV;
            else if (checkedIds.contains(R.id.chipPlans)) filterMode = MODE_PLANS;
            else                                          filterMode = MODE_ALL;
            loadMemories();
        });
    }

    // ── Swipe to delete ────────────────────────────────────────────────────

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            private final ColorDrawable redBg = new ColorDrawable(0xFFF44336);
            private Drawable deleteIcon;

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                Memory memory = adapter.getMemoryAt(pos);
                showDeleteDialog(memory, pos);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int state, boolean active) {
                View item = vh.itemView;
                if (dX < 0) {
                    redBg.setBounds(item.getRight() + (int) dX, item.getTop(),
                            item.getRight(), item.getBottom());
                    redBg.draw(c);
                    if (deleteIcon == null)
                        deleteIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete);
                    if (deleteIcon != null) {
                        int margin = (item.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int top    = item.getTop() + margin;
                        int bottom = top + deleteIcon.getIntrinsicHeight();
                        int right  = item.getRight() - margin;
                        int left   = right - deleteIcon.getIntrinsicWidth();
                        if (item.getRight() + (int) dX < left) {
                            deleteIcon.setBounds(left, top, right, bottom);
                            deleteIcon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, state, active);
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(binding.recyclerView);
    }

    private void showDeleteDialog(Memory memory, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Anıyı Sil")
                .setMessage("\"" + memory.title + "\" kalıcı olarak silinsin mi?")
                .setPositiveButton("Sil", (d, w) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.placeDao().deleteByMemoryId(memory.id);
                        db.photoDao().deleteByMemoryId(memory.id);
                        db.memoryDao().delete(memory);
                        runOnUiThread(this::loadMemories);
                    });
                })
                .setNegativeButton("İptal", (d, w) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(d -> adapter.notifyItemChanged(position))
                .show();
    }

    // ── Data ───────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        // Re-select home tab when returning from other activities
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        loadMemories();
    }

    private void loadMemories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Memory> memories;
            if (filterMode == MODE_FAV)        memories = db.memoryDao().getFavoriteMemories();
            else if (filterMode == MODE_PLANS) memories = db.memoryDao().getFuturePlans();
            else                               memories = db.memoryDao().getAllMemories();

            // "Bu Gün Yıl Önce" banner — only in ALL mode
            Memory onThisDay = null;
            if (filterMode == MODE_ALL) {
                String todayDM = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());
                for (Memory m : memories) {
                    if (m.date != null && m.date.startsWith(todayDM)) { onThisDay = m; break; }
                }
            }

            final List<Memory> result = memories;
            final Memory banner = onThisDay;
            runOnUiThread(() -> {
                allMemories = result;
                applyDisplayList();
                showOnThisDayBanner(banner);
            });
        });
    }

    private void showOnThisDayBanner(Memory memory) {
        if (memory == null) { binding.cardOnThisDay.setVisibility(View.GONE); return; }
        try {
            String[] parts = memory.date.split("\\.");
            int diffYears = 0;
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[2]);
                diffYears = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - year;
            }
            binding.tvOnThisDay.setText(
                    "📅  " + diffYears + " yil once bugun " + memory.city
                    + "'deydin — \"" + memory.title + "\"");
            binding.cardOnThisDay.setVisibility(View.VISIBLE);
            binding.cardOnThisDay.setOnClickListener(v -> {
                Intent i = new Intent(this, DetailActivity.class);
                i.putExtra("memory", memory);
                startActivity(i);
            });
        } catch (Exception ignored) {
            binding.cardOnThisDay.setVisibility(View.GONE);
        }
    }

    private void applyDisplayList() {
        List<Memory> sorted = new ArrayList<>(allMemories);
        switch (currentSortMode) {
            case SORT_OLDEST:    Collections.reverse(sorted); break;
            case SORT_CITY_AZ:   Collections.sort(sorted, (a, b) -> a.city.compareToIgnoreCase(b.city)); break;
            case SORT_FAV_FIRST: Collections.sort(sorted, (a, b) -> Boolean.compare(b.isFavorite, a.isFavorite)); break;
            default: break;
        }
        adapter.updateList(sorted);
        String q = binding.editTextSearch.getText().toString();
        if (!q.isEmpty()) adapter.filter(q);
        updateEmptyState();
    }

    private void updateEmptyState() {
        binding.layoutEmpty.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}

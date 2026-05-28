package com.example.travelog;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelog.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Sıralama modları
    private static final int SORT_NEWEST   = 0;
    private static final int SORT_OLDEST   = 1;
    private static final int SORT_CITY_AZ  = 2;
    private static final int SORT_FAV_FIRST = 3;

    private ActivityMainBinding binding;
    private MemoryAdapter adapter;
    private List<Memory> allMemories = new ArrayList<>();
    private AppDatabase db;

    private int currentSortMode = SORT_NEWEST;
    private boolean showFavoritesOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

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

        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddMemoryActivity.class)));
    }

    // ── Toolbar Menüsü ────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_map) {
            startActivity(new Intent(this, MapActivity.class));
            return true;
        } else if (id == R.id.action_stats) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Sıralama Diyaloğu ─────────────────────────────────────────────────────

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

    // ── Arama ─────────────────────────────────────────────────────────────────

    private void setupSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updateEmptyState();
            }
        });
    }

    // ── Filtre Çipleri ────────────────────────────────────────────────────────

    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            showFavoritesOnly = checkedIds.contains(R.id.chipFavorites);
            loadMemories();
        });
    }

    // ── Sola Kaydırarak Silme ─────────────────────────────────────────────────

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
                    redBg.setBounds(item.getRight() + (int) dX,
                            item.getTop(), item.getRight(), item.getBottom());
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
                        db.memoryDao().delete(memory);
                        runOnUiThread(this::loadMemories);
                    });
                })
                .setNegativeButton("İptal", (d, w) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(d -> adapter.notifyItemChanged(position))
                .show();
    }

    // ── Veri ─────────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        loadMemories();
    }

    private void loadMemories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Memory> memories = showFavoritesOnly
                    ? db.memoryDao().getFavoriteMemories()
                    : db.memoryDao().getAllMemories();

            runOnUiThread(() -> {
                allMemories = memories;
                applyDisplayList();
            });
        });
    }

    /** Sıralama uygulayıp adapter'ı güncelle */
    private void applyDisplayList() {
        List<Memory> sorted = new ArrayList<>(allMemories);

        switch (currentSortMode) {
            case SORT_OLDEST:
                Collections.reverse(sorted);
                break;
            case SORT_CITY_AZ:
                Collections.sort(sorted, (a, b) -> a.city.compareToIgnoreCase(b.city));
                break;
            case SORT_FAV_FIRST:
                Collections.sort(sorted, (a, b) -> Boolean.compare(b.isFavorite, a.isFavorite));
                break;
            default: // SORT_NEWEST — DB zaten DESC sıralı
                break;
        }

        adapter.updateList(sorted);

        String currentSearch = binding.editTextSearch.getText().toString();
        if (!currentSearch.isEmpty()) adapter.filter(currentSearch);

        updateEmptyState();
    }

    private void updateEmptyState() {
        binding.layoutEmpty.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}

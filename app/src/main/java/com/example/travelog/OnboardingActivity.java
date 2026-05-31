package com.example.travelog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.travelog.databinding.ActivityOnboardingBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS = "travelog_prefs";
    public static final String KEY_ONBOARDED = "onboarded";

    private ActivityOnboardingBinding binding;

    private static final String[] ICONS    = {"✈️", "🗺️", "📊"};
    private static final String[] TITLES   = {
            "Anılarını Kaydet",
            "Haritada Keşfet",
            "İstatistiklerini Gör"
    };
    private static final String[] DESCS = {
            "Gezdiklerini fotoğraf, hava durumu ve ünlü yerlerle birlikte kayıt altına al.",
            "Tüm anılarını dünya haritasında pinler üzerinde gör ve rota oluştur.",
            "Kaç şehir, kaç ülke, kaç favori — tüm seyahat istatistiklerin bir arada."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.viewPager.setAdapter(new SlideAdapter());
        new TabLayoutMediator(binding.dotsIndicator, binding.viewPager,
                (tab, pos) -> {}).attach();

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                boolean isLast = pos == TITLES.length - 1;
                binding.btnNext.setText(isLast ? "Başla 🚀" : "İleri");
            }
        });

        binding.btnSkip.setOnClickListener(v -> finish());
        binding.btnNext.setOnClickListener(v -> {
            int cur = binding.viewPager.getCurrentItem();
            if (cur < TITLES.length - 1) {
                binding.viewPager.setCurrentItem(cur + 1);
            } else {
                finish();
            }
        });
    }

    @Override public void finish() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_ONBOARDED, true).apply();
        startActivity(new Intent(this, MainActivity.class));
        super.finish();
    }

    // ── Slayt Adapter ─────────────────────────────────────────────────────────

    private class SlideAdapter extends RecyclerView.Adapter<SlideVH> {
        @NonNull @Override
        public SlideVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_slide, parent, false);
            return new SlideVH(v);
        }
        @Override public void onBindViewHolder(@NonNull SlideVH h, int pos) {
            h.tvIcon.setText(ICONS[pos]);
            h.tvTitle.setText(TITLES[pos]);
            h.tvDesc.setText(DESCS[pos]);
        }
        @Override public int getItemCount() { return TITLES.length; }
    }

    static class SlideVH extends RecyclerView.ViewHolder {
        final TextView tvIcon, tvTitle, tvDesc;
        SlideVH(@NonNull View v) {
            super(v);
            tvIcon  = v.findViewById(R.id.tvSlideIcon);
            tvTitle = v.findViewById(R.id.tvSlideTitle);
            tvDesc  = v.findViewById(R.id.tvSlideDesc);
        }
    }
}

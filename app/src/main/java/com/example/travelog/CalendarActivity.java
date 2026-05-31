package com.example.travelog;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelog.databinding.ActivityCalendarBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class CalendarActivity extends AppCompatActivity {

    private ActivityCalendarBinding binding;
    private final Calendar displayed = Calendar.getInstance();
    private final List<Memory> allDated = new ArrayList<>();
    private final Locale tr = new Locale("tr", "TR");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        displayed.set(Calendar.DAY_OF_MONTH, 1);

        binding.btnPrevMonth.setOnClickListener(v -> {
            displayed.add(Calendar.MONTH, -1);
            buildGrid();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            displayed.add(Calendar.MONTH, 1);
            buildGrid();
        });

        loadData();
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Memory> list = AppDatabase.getInstance(this).memoryDao().getAllWithDate();
            runOnUiThread(() -> {
                allDated.clear();
                allDated.addAll(list);
                buildGrid();
            });
        });
    }

    /** "dd.MM.yyyy" → int[]{day, month(1-12), year} ; hatalıysa null */
    private int[] parseDate(String date) {
        if (date == null) return null;
        String[] p = date.split("\\.");
        if (p.length != 3) return null;
        try {
            return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void buildGrid() {
        binding.gridCalendar.removeAllViews();
        binding.tvMonthYear.setText(
                new SimpleDateFormat("MMMM yyyy", tr).format(displayed.getTime()));

        int month = displayed.get(Calendar.MONTH) + 1; // 1-12
        int year  = displayed.get(Calendar.YEAR);

        // Gün → o güne ait anılar
        Map<Integer, List<Memory>> dayMap = new HashMap<>();
        for (Memory m : allDated) {
            int[] d = parseDate(m.date);
            if (d != null && d[1] == month && d[2] == year) {
                List<Memory> l = dayMap.get(d[0]);
                if (l == null) { l = new ArrayList<>(); dayMap.put(d[0], l); }
                l.add(m);
            }
        }

        Calendar first = Calendar.getInstance();
        first.set(year, month - 1, 1);
        int dow = first.get(Calendar.DAY_OF_WEEK);    // SUN=1..SAT=7
        int leading = (dow + 5) % 7;                  // Pazartesi=0
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        boolean isThisMonth = today.get(Calendar.MONTH) + 1 == month
                && today.get(Calendar.YEAR) == year;
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        int density = (int) getResources().getDisplayMetrics().density;
        LinearLayout week = null;
        int cellIndex = 0;
        int totalCells = leading + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0) * 7;

        for (int i = 0; i < rows; i++) {
            if (i % 7 == 0) {
                week = new LinearLayout(this);
                week.setOrientation(LinearLayout.HORIZONTAL);
                week.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                binding.gridCalendar.addView(week);
            }

            int dayNum = i - leading + 1;
            if (i < leading || dayNum > daysInMonth) {
                week.addView(makeEmptyCell());
            } else {
                List<Memory> items = dayMap.get(dayNum);
                boolean isToday = isThisMonth && dayNum == todayDay;
                week.addView(makeDayCell(dayNum, items, isToday, density));
            }
            cellIndex++;
        }
    }

    private View makeEmptyCell() {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View makeDayCell(int dayNum, List<Memory> items, boolean isToday, int density) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setMinimumHeight(48 * density);
        cell.setPadding(0, 4 * density, 0, 4 * density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cell.setLayoutParams(lp);
        cell.setClickable(true);
        cell.setBackgroundResource(android.R.color.transparent);

        TextView num = new TextView(this);
        num.setText(String.valueOf(dayNum));
        num.setTextSize(15f);
        num.setGravity(Gravity.CENTER);
        num.setTextColor(getColor(R.color.text_primary));
        if (isToday) {
            num.setBackgroundResource(R.drawable.bg_today_circle);
            num.setWidth(30 * density);
            num.setHeight(30 * density);
            num.setTextColor(getColor(R.color.primary));
            num.setTypeface(num.getTypeface(), android.graphics.Typeface.BOLD);
        }
        cell.addView(num);

        // Nokta satırı
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dotsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dotsLp.topMargin = 3 * density;
        dots.setLayoutParams(dotsLp);

        boolean hasMemory = false, hasPlan = false;
        if (items != null) {
            for (Memory m : items) {
                if (m.isFuturePlan) hasPlan = true; else hasMemory = true;
            }
        }
        if (hasMemory) dots.addView(makeDot(R.drawable.dot_memory, density));
        if (hasPlan)   dots.addView(makeDot(R.drawable.dot_plan, density));
        cell.addView(dots);

        cell.setOnClickListener(v -> showDayDetail(dayNum, items));
        return cell;
    }

    private View makeDot(int drawable, int density) {
        View dot = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(6 * density, 6 * density);
        lp.setMargins(2 * density, 0, 2 * density, 0);
        dot.setLayoutParams(lp);
        dot.setBackgroundResource(drawable);
        return dot;
    }

    private void showDayDetail(int dayNum, List<Memory> items) {
        int month = displayed.get(Calendar.MONTH) + 1;
        int year  = displayed.get(Calendar.YEAR);
        binding.tvDayDetailTitle.setText(String.format(Locale.getDefault(),
                "%02d.%02d.%04d", dayNum, month, year));
        binding.layoutDayDetail.removeAllViews();

        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Bu güne ait kayıt yok");
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(14f);
            empty.setPadding(0, 8, 0, 8);
            binding.layoutDayDetail.addView(empty);
            return;
        }

        int density = (int) getResources().getDisplayMetrics().density;
        for (Memory m : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(12 * density, 10 * density, 12 * density, 10 * density);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = 8 * density;
            row.setLayoutParams(rp);
            row.setBackgroundColor(getColor(R.color.surface));
            row.setClickable(true);

            TextView t = new TextView(this);
            String tag = m.isFuturePlan ? "📅 " : "📷 ";
            t.setText(tag + (m.title != null ? m.title : ""));
            t.setTextColor(getColor(R.color.text_primary));
            t.setTextSize(15f);
            t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
            row.addView(t);

            TextView sub = new TextView(this);
            String loc = (m.city != null ? m.city : "");
            if (m.country != null && !m.country.isEmpty()) loc += " · " + m.country;
            sub.setText(loc);
            sub.setTextColor(getColor(R.color.text_secondary));
            sub.setTextSize(13f);
            row.addView(sub);

            row.setOnClickListener(v -> {
                Intent i = new Intent(this, DetailActivity.class);
                i.putExtra("memory", m);
                startActivity(i);
            });
            binding.layoutDayDetail.addView(row);
        }
    }
}

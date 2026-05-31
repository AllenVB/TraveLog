package com.example.travelog;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Memory memory);
    }

    private List<Memory> fullList;
    private List<Memory> filteredList;
    private final OnItemClickListener listener;

    public MemoryAdapter(List<Memory> memoryList, OnItemClickListener listener) {
        this.fullList = new ArrayList<>(memoryList);
        this.filteredList = new ArrayList<>(memoryList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory, parent, false);
        return new MemoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        Memory memory = filteredList.get(position);

        holder.textViewTitle.setText(memory.title != null ? memory.title : "");
        holder.textViewCity.setText(memory.city   != null ? memory.city.toUpperCase() : "");
        holder.textViewDate.setText(memory.date   != null ? "📅 " + memory.date : "");
        holder.textViewWeather.setText(
                TextUtils.isEmpty(memory.weather) ? "" : "🌤 " + memory.weather);

        // Favorite heart — top-right
        holder.ivFavorite.setVisibility(memory.isFavorite ? View.VISIBLE : View.GONE);

        // Plan badge — top-left
        holder.tvPlanBadge.setVisibility(memory.isFuturePlan ? View.VISIBLE : View.GONE);

        // Cover photo
        if (!TextUtils.isEmpty(memory.imageUri)) {
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(memory.imageUri))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.color.image_placeholder)
                    .into(holder.imageView);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(R.color.image_placeholder)
                    .into(holder.imageView);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(memory));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public Memory getMemoryAt(int position) {
        return filteredList.get(position);
    }

    /** Animated update via DiffUtil */
    public void updateList(List<Memory> newList) {
        MemoryDiffCallback callback = new MemoryDiffCallback(filteredList, newList);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);
        this.fullList = new ArrayList<>(newList);
        this.filteredList = new ArrayList<>(newList);
        result.dispatchUpdatesTo(this);
    }

    /** Instant filter by title or city */
    public void filter(String query) {
        List<Memory> newFiltered = new ArrayList<>();
        if (TextUtils.isEmpty(query.trim())) {
            newFiltered.addAll(fullList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Memory m : fullList) {
                boolean titleMatch = m.title != null && m.title.toLowerCase().contains(lowerQuery);
                boolean cityMatch  = m.city  != null && m.city.toLowerCase().contains(lowerQuery);
                if (titleMatch || cityMatch) {
                    newFiltered.add(m);
                }
            }
        }
        filteredList = newFiltered;
        notifyDataSetChanged();
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, ivFavorite;
        TextView textViewTitle, textViewCity, textViewDate, textViewWeather, tvPlanBadge;

        MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView       = itemView.findViewById(R.id.imageViewItem);
            ivFavorite      = itemView.findViewById(R.id.ivFavoriteIndicator);
            tvPlanBadge     = itemView.findViewById(R.id.tvPlanBadgeCard);
            textViewTitle   = itemView.findViewById(R.id.textViewTitleItem);
            textViewCity    = itemView.findViewById(R.id.textViewCityItem);
            textViewDate    = itemView.findViewById(R.id.textViewDateItem);
            textViewWeather = itemView.findViewById(R.id.textViewWeatherItem);
        }
    }
}

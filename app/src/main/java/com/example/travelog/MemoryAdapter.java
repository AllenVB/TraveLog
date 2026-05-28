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

        holder.textViewTitle.setText(memory.title);
        holder.textViewCity.setText("📍 " + memory.city);
        holder.textViewDate.setText("📅 " + memory.date);
        holder.textViewWeather.setText(
                TextUtils.isEmpty(memory.weather) ? "" : "🌤 " + memory.weather);

        // Favori ikonu
        holder.ivFavorite.setVisibility(memory.isFavorite ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(memory.imageUri)) {
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(memory.imageUri))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(android.R.color.darker_gray)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(android.R.color.darker_gray);
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

    /** DiffUtil ile animasyonlu güncelleme */
    public void updateList(List<Memory> newList) {
        MemoryDiffCallback callback = new MemoryDiffCallback(filteredList, newList);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);
        this.fullList = new ArrayList<>(newList);
        this.filteredList = new ArrayList<>(newList);
        result.dispatchUpdatesTo(this);
    }

    /** Başlık veya şehre göre anlık filtreleme */
    public void filter(String query) {
        List<Memory> newFiltered = new ArrayList<>();
        if (TextUtils.isEmpty(query.trim())) {
            newFiltered.addAll(fullList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Memory m : fullList) {
                if (m.title.toLowerCase().contains(lowerQuery)
                        || m.city.toLowerCase().contains(lowerQuery)) {
                    newFiltered.add(m);
                }
            }
        }
        filteredList = newFiltered;
        notifyDataSetChanged();
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, ivFavorite;
        TextView textViewTitle, textViewCity, textViewDate, textViewWeather;

        MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            ivFavorite = itemView.findViewById(R.id.ivFavoriteIndicator);
            textViewTitle = itemView.findViewById(R.id.textViewTitleItem);
            textViewCity = itemView.findViewById(R.id.textViewCityItem);
            textViewDate = itemView.findViewById(R.id.textViewDateItem);
            textViewWeather = itemView.findViewById(R.id.textViewWeatherItem);
        }
    }
}

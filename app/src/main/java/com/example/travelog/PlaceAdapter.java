package com.example.travelog;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    public interface Listener {
        void onVisitedToggled(int position, boolean isVisited);
        void onPickPhoto(int position);
    }

    private final List<Place> places;
    private final Listener listener;

    public PlaceAdapter(List<Place> places, Listener listener) {
        this.places = places;
        this.listener = listener;
    }

    public Place getPlace(int position) {
        return places.get(position);
    }

    /** Fotoğraf URI'si güncellendikten sonra adapter'ı yenile */
    public void updatePlacePhoto(int position, String uri) {
        if (position >= 0 && position < places.size()) {
            places.get(position).photoUri = uri;
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place place = places.get(position);

        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvName.setText(place.name);

        // Listener'ı geçici olarak kaldır (bind sırasında tetiklenmesin)
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(place.isVisited);
        holder.checkbox.setOnCheckedChangeListener((btn, checked) -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onVisitedToggled(pos, checked);
                holder.btnCamera.setVisibility(checked ? View.VISIBLE : View.GONE);
                if (!checked) {
                    holder.ivPhoto.setVisibility(View.GONE);
                }
            }
        });

        // Kamera butonu: sadece "Gezdim" işaretliyse görünür
        holder.btnCamera.setVisibility(place.isVisited ? View.VISIBLE : View.GONE);
        holder.btnCamera.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onPickPhoto(pos);
            }
        });

        // Fotoğraf thumbnail
        if (!TextUtils.isEmpty(place.photoUri)) {
            holder.ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(holder.ivPhoto.getContext())
                    .load(Uri.parse(place.photoUri))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.color.image_placeholder)
                    .into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNumber, tvName;
        final CheckBox checkbox;
        final ImageButton btnCamera;
        final ImageView ivPhoto;

        ViewHolder(@NonNull View v) {
            super(v);
            tvNumber  = v.findViewById(R.id.tvPlaceNumber);
            tvName    = v.findViewById(R.id.tvPlaceName);
            checkbox  = v.findViewById(R.id.checkboxVisited);
            btnCamera = v.findViewById(R.id.btnAddPhoto);
            ivPhoto   = v.findViewById(R.id.ivPlacePhoto);
        }
    }
}

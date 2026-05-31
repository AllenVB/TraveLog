package com.example.travelog;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

/** Yatay fotoğraf galerisi için adapter. */
public class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onAddPhoto();
        void onDeletePhoto(int position);
    }

    private static final int TYPE_ADD    = 0;
    private static final int TYPE_PHOTO  = 1;

    private final List<MemoryPhoto> photos;
    private final Listener listener;
    private final boolean editable; // true → + ve × butonları göster

    public PhotoAdapter(List<MemoryPhoto> photos, Listener listener, boolean editable) {
        this.photos   = photos;
        this.listener = listener;
        this.editable = editable;
    }

    @Override public int getItemViewType(int pos) {
        return (editable && pos == photos.size()) ? TYPE_ADD : TYPE_PHOTO;
    }

    @Override public int getItemCount() { return photos.size() + (editable ? 1 : 0); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View v = inf.inflate(R.layout.item_photo_add, parent, false);
            return new AddVH(v);
        }
        View v = inf.inflate(R.layout.item_photo_gallery, parent, false);
        return new PhotoVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        if (holder instanceof AddVH) {
            ((AddVH) holder).itemView.setOnClickListener(v -> listener.onAddPhoto());
            return;
        }
        MemoryPhoto photo = photos.get(pos);
        PhotoVH vh = (PhotoVH) holder;
        Glide.with(vh.iv.getContext())
                .load(Uri.parse(photo.uri))
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.color.image_placeholder)
                .into(vh.iv);
        vh.btnDelete.setVisibility(editable ? View.VISIBLE : View.GONE);
        vh.btnDelete.setOnClickListener(v -> {
            int p = holder.getAdapterPosition();
            if (p != RecyclerView.NO_POSITION) listener.onDeletePhoto(p);
        });
    }

    static class PhotoVH extends RecyclerView.ViewHolder {
        final ImageView iv;
        final ImageButton btnDelete;
        PhotoVH(@NonNull View v) {
            super(v);
            iv        = v.findViewById(R.id.ivGalleryPhoto);
            btnDelete = v.findViewById(R.id.btnDeletePhoto);
        }
    }
    static class AddVH extends RecyclerView.ViewHolder {
        AddVH(@NonNull View v) { super(v); }
    }
}

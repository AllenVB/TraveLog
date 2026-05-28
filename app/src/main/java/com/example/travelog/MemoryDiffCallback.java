package com.example.travelog;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class MemoryDiffCallback extends DiffUtil.Callback {

    private final List<Memory> oldList;
    private final List<Memory> newList;

    public MemoryDiffCallback(List<Memory> oldList, List<Memory> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        // Aynı kayıt mı? (ID ile karşılaştır)
        return oldList.get(oldPos).id == newList.get(newPos).id;
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        // İçerik değişti mi?
        Memory o = oldList.get(oldPos);
        Memory n = newList.get(newPos);
        return o.title.equals(n.title)
                && o.city.equals(n.city)
                && o.isFavorite == n.isFavorite
                && o.weather.equals(n.weather);
    }
}

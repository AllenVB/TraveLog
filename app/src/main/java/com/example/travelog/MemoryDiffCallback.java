package com.example.travelog;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

public class MemoryDiffCallback extends DiffUtil.Callback {

    private final List<Memory> oldList;
    private final List<Memory> newList;

    public MemoryDiffCallback(List<Memory> oldList, List<Memory> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() { return oldList.size(); }

    @Override
    public int getNewListSize() { return newList.size(); }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        return oldList.get(oldPos).id == newList.get(newPos).id;
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        Memory o = oldList.get(oldPos);
        Memory n = newList.get(newPos);
        // Objects.equals() is null-safe (returns true for null==null)
        return Objects.equals(o.title,     n.title)
                && Objects.equals(o.city,  n.city)
                && Objects.equals(o.weather, n.weather)
                && o.isFavorite   == n.isFavorite
                && o.isFuturePlan == n.isFuturePlan;
    }
}

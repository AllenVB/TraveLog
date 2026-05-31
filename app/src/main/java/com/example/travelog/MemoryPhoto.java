package com.example.travelog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Bir anıya ait ek fotoğraflar (kapak fotoğrafı hariç). */
@Entity(tableName = "memory_photos")
public class MemoryPhoto {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int memoryId;
    public String uri;

    public MemoryPhoto(int memoryId, String uri) {
        this.memoryId = memoryId;
        this.uri = uri;
    }
}

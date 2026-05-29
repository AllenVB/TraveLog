package com.example.travelog;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "places")
public class Place implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Bağlı olduğu anının id'si */
    public int memoryId;

    /** Yer adı (Wikipedia başlığı) */
    public String name;

    /** Kullanıcı burayı gezdi mi */
    public boolean isVisited;

    /** Kullanıcının eklediği fotoğraf URI (null olabilir) */
    @Nullable
    public String photoUri;

    public Place(int memoryId, String name) {
        this.memoryId = memoryId;
        this.name = name;
        this.isVisited = false;
        this.photoUri = null;
    }
}

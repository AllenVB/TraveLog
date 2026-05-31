package com.example.travelog;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "memories")
public class Memory implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public String city;
    public String country;
    public String imageUri;
    public String weather;
    public String date;

    @ColumnInfo(defaultValue = "0")
    public boolean isFavorite;

    /** true → gezi planı (henüz gidilmedi); false → gerçek anı */
    @ColumnInfo(defaultValue = "0")
    public boolean isFuturePlan;

    public Memory(String title, String description, String city,
                  String imageUri, String weather, String date) {
        this.title = title;
        this.description = description;
        this.city = city;
        this.imageUri = imageUri;
        this.weather = weather;
        this.date = date;
        this.isFavorite = false;
        this.isFuturePlan = false;
    }
}

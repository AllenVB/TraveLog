package com.example.travelog;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemoryDao {

    @Insert
    long insert(Memory memory);

    @Update
    void update(Memory memory);

    @Delete
    void delete(Memory memory);

    @Query("SELECT * FROM memories ORDER BY id DESC")
    List<Memory> getAllMemories();

    @Query("SELECT * FROM memories WHERE isFavorite = 1 ORDER BY id DESC")
    List<Memory> getFavoriteMemories();

    // İstatistik sorguları
    @Query("SELECT COUNT(*) FROM memories")
    int getTotalCount();

    @Query("SELECT COUNT(DISTINCT city) FROM memories")
    int getUniqueCityCount();

    @Query("SELECT COUNT(*) FROM memories WHERE isFavorite = 1")
    int getFavoriteCount();

    @Query("SELECT city, COUNT(*) as cnt FROM memories GROUP BY city ORDER BY cnt DESC")
    List<CityCount> getCityCounts();

    // Şehir-sayı verisi için ara sınıf
    class CityCount {
        public String city;
        public int cnt;
    }
}

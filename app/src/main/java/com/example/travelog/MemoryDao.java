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

    @Query("SELECT * FROM memories WHERE isFavorite = 1 AND isFuturePlan = 0 ORDER BY id DESC")
    List<Memory> getFavoriteMemories();

    // İstatistik sorguları
    @Query("SELECT COUNT(*) FROM memories WHERE isFuturePlan = 0")
    int getTotalCount();

    @Query("SELECT COUNT(DISTINCT city) FROM memories WHERE isFuturePlan = 0")
    int getUniqueCityCount();

    @Query("SELECT COUNT(*) FROM memories WHERE isFavorite = 1 AND isFuturePlan = 0")
    int getFavoriteCount();

    @Query("SELECT * FROM memories WHERE isFuturePlan = 0 ORDER BY id DESC")
    List<Memory> getAllMemories();

    @Query("SELECT * FROM memories WHERE isFuturePlan = 1 ORDER BY date ASC")
    List<Memory> getFuturePlans();

    @Query("SELECT city, COUNT(*) as cnt FROM memories WHERE isFuturePlan = 0 GROUP BY city ORDER BY cnt DESC")
    List<CityCount> getCityCounts();

    @Query("SELECT COUNT(*) FROM memories WHERE isFuturePlan = 1")
    int getFuturePlanCount();

    // Şehir-sayı verisi için ara sınıf
    class CityCount {
        public String city;
        public int cnt;
    }
}

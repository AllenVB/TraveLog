package com.example.travelog;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PlaceDao {

    @Insert
    void insertAll(List<Place> places);

    @Update
    void update(Place place);

    @Query("SELECT * FROM places WHERE memoryId = :memoryId ORDER BY id ASC")
    List<Place> getPlacesForMemory(int memoryId);

    @Query("DELETE FROM places WHERE memoryId = :memoryId")
    void deleteByMemoryId(int memoryId);
}

package com.example.travelog;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MemoryPhotoDao {

    @Insert
    void insert(MemoryPhoto photo);

    @Delete
    void delete(MemoryPhoto photo);

    @Query("SELECT * FROM memory_photos WHERE memoryId = :memoryId ORDER BY id ASC")
    List<MemoryPhoto> getPhotosForMemory(int memoryId);

    @Query("DELETE FROM memory_photos WHERE memoryId = :memoryId")
    void deleteByMemoryId(int memoryId);
}

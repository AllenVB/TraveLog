package com.example.travelog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Memory.class, Place.class, MemoryPhoto.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract MemoryDao memoryDao();
    public abstract PlaceDao placeDao();
    public abstract MemoryPhotoDao photoDao();

    // ── Versiyon 1 → 2 : isFavorite kolonu ──────────────────────────────────
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "ALTER TABLE memories ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
            );
        }
    };

    // ── Versiyon 2 → 3 : places tablosu ─────────────────────────────────────
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `places` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`memoryId` INTEGER NOT NULL, " +
                "`name` TEXT, " +
                "`isVisited` INTEGER NOT NULL DEFAULT 0, " +
                "`photoUri` TEXT)"
            );
        }
    };

    // ── Versiyon 3 → 4 : isFuturePlan alanı + memory_photos tablosu ─────────────
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "ALTER TABLE memories ADD COLUMN isFuturePlan INTEGER NOT NULL DEFAULT 0"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `memory_photos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`memoryId` INTEGER NOT NULL, " +
                "`uri` TEXT NOT NULL)"
            );
        }
    };

    // ── Versiyon 4 → 5 : country (ülke) alanı ───────────────────────────────
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "ALTER TABLE memories ADD COLUMN country TEXT"
            );
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "travel_log_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}

package com.aksatoskar.ycsplassignment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aksatoskar.ycsplassignment.model.LocationDetails

@Database(entities = [LocationDetails::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
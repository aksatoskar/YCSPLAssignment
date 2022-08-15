package com.aksatoskar.ycsplassignment.data.local

import androidx.room.*
import com.aksatoskar.ycsplassignment.model.LocationDetails

@Dao
interface LocationDao {

    @Query("SELECT * FROM LocationDetails")
    fun getAll(): List<LocationDetails>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(profiles: LocationDetails)

    @Update
    fun update(profileDetails: LocationDetails)
}
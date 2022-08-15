package com.aksatoskar.ycsplassignment.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "LocationDetails", primaryKeys = ["latitude","longitude","propertyName"])
data class LocationDetails (
    @NonNull
    val latitude : String,
    @NonNull
    val longitude : String,
    @NonNull
    val propertyName: String
)

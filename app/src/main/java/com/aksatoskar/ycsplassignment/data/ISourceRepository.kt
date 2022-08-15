package com.aksatoskar.ycsplassignment.data

import com.aksatoskar.ycsplassignment.model.LocationDetails
import com.aksatoskar.ycsplassignment.model.Resource
import kotlinx.coroutines.flow.Flow


interface ISourceRepository {
    suspend fun insertLocation(profileDetails: LocationDetails): Flow<Resource<Boolean>>

    suspend fun getAllLocations(): Flow<Resource<List<LocationDetails>>>
}
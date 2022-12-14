package com.aksatoskar.ycsplassignment.data

import com.aksatoskar.ycsplassignment.model.LocationDetails
import com.aksatoskar.ycsplassignment.model.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SourceRepositoryImpl @Inject constructor(
    private var dataSource: IDataSource,
    private var dispatcher: CoroutineDispatcher
) : ISourceRepository {
    override suspend fun insertLocation(locationDetails: LocationDetails): Flow<Resource<Boolean>> {
        return dataSource.insertLocation(locationDetails)
    }

    override suspend fun getAllLocations(): Flow<Resource<List<LocationDetails>>> {
        return dataSource.getAllLocations()
    }
}
package com.aksatoskar.ycsplassignment.data

import com.aksatoskar.ycsplassignment.data.local.LocationDao
import com.aksatoskar.ycsplassignment.model.LocationDetails
import com.aksatoskar.ycsplassignment.model.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DataSourceImpl @Inject constructor(
    private val locationDao: LocationDao,
    private var ioDispatcher: CoroutineDispatcher
) : IDataSource {
    override suspend fun insertLocation(profileDetails: LocationDetails): Flow<Resource<Boolean>> {
        return flow {
            emit(Resource.loading())
            locationDao.insert(profileDetails)
            emit(Resource.success(true))
        }.flowOn(ioDispatcher)
    }

    override suspend fun getAllLocations(): Flow<Resource<List<LocationDetails>>> {
        return flow {
            emit(Resource.loading())
            emit(Resource.success(locationDao.getAll()))
        }.flowOn(ioDispatcher)
    }
}
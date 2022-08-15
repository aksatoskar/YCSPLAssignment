package com.aksatoskar.ycsplassignment.di

import com.aksatoskar.ycsplassignment.data.DataSourceImpl
import com.aksatoskar.ycsplassignment.data.IDataSource
import com.aksatoskar.ycsplassignment.data.ISourceRepository
import com.aksatoskar.ycsplassignment.data.SourceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Provides
    fun provideIOCoroutineDispatcher(): CoroutineDispatcher {
        return ioDispatcher
    }

    @Singleton
    @Provides
    fun provideUpiSourceRepositoryImpl(source: SourceRepositoryImpl): ISourceRepository {
        return source
    }

    @Singleton
    @Provides
    fun provideUpiDataSource(dataSourceImpl: DataSourceImpl): IDataSource {
        return dataSourceImpl
    }
}
package com.aksatoskar.ycsplassignment.ui.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aksatoskar.ycsplassignment.data.ISourceRepository
import com.aksatoskar.ycsplassignment.model.LocationDetails
import com.aksatoskar.ycsplassignment.model.Resource
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private var sourceRepository: ISourceRepository
) : ViewModel() {
    private val _stateFlow = MutableStateFlow<Resource<List<LocationDetails>>>(Resource.loading())
    val stateFlow: StateFlow<Resource<List<LocationDetails>>>
        get() = _stateFlow

    private val _insertLocationItem = MutableStateFlow<Resource<Boolean>>(Resource.loading())
    val insertLocationItem: StateFlow<Resource<Boolean>>
        get() = _insertLocationItem

    fun fetchLocations() {
        viewModelScope.launch {
            sourceRepository.getAllLocations().collect {
                _stateFlow.value = it
            }
        }
    }

    fun insertLocation(location: LatLng, name: String) {
        val locationDetails = LocationDetails(latitude = location.latitude, longitude = location.longitude, propertyName = name)
        viewModelScope.launch {
            sourceRepository.insertLocation(locationDetails).collect {
                _insertLocationItem.value = it
            }
        }
    }

}
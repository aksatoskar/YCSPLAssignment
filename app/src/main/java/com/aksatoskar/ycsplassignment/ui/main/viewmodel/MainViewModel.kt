package com.aksatoskar.ycsplassignment.ui.main.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aksatoskar.ycsplassignment.data.ISourceRepository
import com.aksatoskar.ycsplassignment.model.LocationDetails
import com.aksatoskar.ycsplassignment.model.Resource
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private var sourceRepository: ISourceRepository
) : ViewModel() {

    var lastKnownLocation: Location? = null
    private var _mapCenterMarkerLatLng: LatLng? = null

    private val _propertyName = MutableLiveData("")
    val propertyName: LiveData<String> = _propertyName

    private val _propertyCoordinates = MutableLiveData("")
    val propertyCoordinates: LiveData<String> = _propertyCoordinates

    private val _bottomSheetState = MutableLiveData(BottomSheetBehavior.STATE_HIDDEN)
    val bottomSheetState: LiveData<Int> = _bottomSheetState

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

    private fun insertLocation(location: LatLng, name: String) {
        val locationDetails = LocationDetails(latitude = location.latitude, longitude = location.longitude, propertyName = name)
        viewModelScope.launch {
            sourceRepository.insertLocation(locationDetails).collect {
                if (it.status == Resource.Status.SUCCESS) {
                    _propertyName.value = ""
                    _bottomSheetState.value = BottomSheetBehavior.STATE_HIDDEN
                }
                _insertLocationItem.value = it
            }
        }
    }

    fun onProceed() {
        _mapCenterMarkerLatLng?.let {
            if (!_propertyName.value.isNullOrBlank()) {
                insertLocation(it, _propertyName.value!!)
            }
        }
    }

    fun onPropertyNameTextChanged(text: CharSequence) {
        _propertyName.value = text.toString()
    }

    fun setPropertyCoordinates(coordinates: LatLng?) {
        _mapCenterMarkerLatLng = coordinates
        _propertyCoordinates.value = "${coordinates?.latitude}, ${coordinates?.longitude}"
    }

    fun getLastKnownPropertyCoordinates(): LatLng? {
        return _mapCenterMarkerLatLng
    }

    fun toggleBottomSheet() {
        if (_bottomSheetState.value == BottomSheetBehavior.STATE_HIDDEN) {
            _bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED
        } else {
            _propertyName.value = ""
            _bottomSheetState.value = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    fun onBottomSheetStateChanged(state: Int) {
        _bottomSheetState.value = state
    }
}
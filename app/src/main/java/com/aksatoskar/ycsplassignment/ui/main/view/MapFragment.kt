package com.aksatoskar.ycsplassignment.ui.main.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aksatoskar.ycsplassignment.R
import com.aksatoskar.ycsplassignment.databinding.FragmentMapBinding
import com.aksatoskar.ycsplassignment.model.Resource
import com.aksatoskar.ycsplassignment.ui.main.viewmodel.MainViewModel
import com.aksatoskar.ycsplassignment.util.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var sheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    private var showSettingsForPermission = false

    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
        subscribeUI()
        setListeners()
    }

    private fun setListeners() {
        sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_EXPANDED) {
                    viewModel.onBottomSheetStateChanged(newState)
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                //No handling required
            }
        })
    }

    private fun initializeUI() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheetContainer)
        sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        binding.bottomSheet.lifecycleOwner = this.viewLifecycleOwner
        binding.bottomSheet.mainViewModel = viewModel
        binding.lifecycleOwner = this.viewLifecycleOwner
        binding.mainViewModel = viewModel
        binding.incMapContent.lifecycleOwner = this.viewLifecycleOwner
        binding.incMapContent.mainViewModel = viewModel
    }

    private fun subscribeUI() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateFlow.collect { resource ->
                    when (resource.status) {
                        Resource.Status.SUCCESS -> {
                            resource.data?.let { list ->
                                list.map { locationDetails ->
                                    val position = LatLng(locationDetails.latitude, locationDetails.longitude)
                                    map?.addMarker(
                                        MarkerOptions()
                                            .position(position)
                                            .title(locationDetails.propertyName)
                                    )

                                }
                            }
                            binding.loader.hide()
                        }
                        Resource.Status.LOADING -> {
                            binding.loader.show()
                        }
                        Resource.Status.ERROR -> {
                            binding.loader.hide()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                lifecycleScope.launchWhenStarted {
                    viewModel.insertLocationItem.collect { result ->
                        when (result.status) {
                            Resource.Status.SUCCESS -> {
                                binding.loader.hide()
                                activity?.hideKeyboard(binding.root)
                                viewModel.fetchLocations()
                            }
                            Resource.Status.LOADING -> {
                                binding.loader.show()
                            }
                            Resource.Status.ERROR -> {
                                binding.loader.hide()
                            }
                        }
                    }
                }
            }
        }

        viewModel.propertyName.observe(viewLifecycleOwner) { name ->
            if (!name.isNullOrBlank()) {
                binding.bottomSheet.tlPropertyNameHeader.helperText = ""
            } else {
                binding.bottomSheet.tlPropertyNameHeader.helperText =
                    resources.getString(R.string.empty_property_name_validation)
            }
        }

        viewModel.propertyCoordinates.observe(viewLifecycleOwner) { coordinates ->
            if (!coordinates.isNullOrBlank()) {
                binding.bottomSheet.tlPropertyCoordinatesHeader.helperText = ""
            } else {
                binding.bottomSheet.tlPropertyCoordinatesHeader.helperText =
                    resources.getString(R.string.empty_property_coordinates_validation)
            }
        }

        viewModel.bottomSheetState.observe(viewLifecycleOwner) { state ->
            if (activity?.isKeyboardOpen(binding.root) == true) {
                activity?.hideKeyboard(binding.root)
                binding.root.postDelayed({
                    sheetBehavior?.state = state
                }, KEYBOARD_DETECTION_DELAY)
            } else {
                sheetBehavior?.state = state
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        activity?.hideKeyboard(binding.root)
        binding.loader.hide()
        if (showSettingsForPermission) {
            showSettingsForPermission = false
            if (isLocationPermissionGranted()) {
                locationPermissionGranted()
            } else {
                openSettingsForPermission()
            }
        }
    }

    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (isLocationPermissionGranted()) {
                val propertyCoordinates = viewModel.getLastKnownPropertyCoordinates()
                if (propertyCoordinates != null && propertyCoordinates.latitude != 0.0 && propertyCoordinates.longitude != 0.0) {
                    map?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(propertyCoordinates.latitude,
                                propertyCoordinates.longitude), DEFAULT_ZOOM.toFloat()))
                    return
                }
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
                getDeviceLocation()
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                viewModel.lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        try {
            if (isLocationPermissionGranted()) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        viewModel.lastKnownLocation = task.result
                        if (viewModel.lastKnownLocation != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                LatLng(viewModel.lastKnownLocation!!.latitude,
                                    viewModel.lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.map = googleMap
        this.map?.setOnCameraIdleListener(this)
        this.map?.setOnCameraMoveListener (this)
        updateLocationUI()
        viewModel.fetchLocations()
    }

    /**
     * Implementation of Google Map Camera listeners
     */
    override fun onCameraMove() {
        //No handling required
    }

    override fun onCameraIdle() {
        viewModel.setPropertyCoordinates(map?.cameraPosition?.target)
    }

    /**
     * Location Permission Handling
     */
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                binding.incMapContent.mainContainer.showSnackbar(
                    R.string.location_permission_granted,
                    Snackbar.LENGTH_SHORT
                )
                locationPermissionGranted()
            } else {
                requestLocationPermission()
            }
        }

    private fun requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            binding.incMapContent.mainContainer.showSnackbar(
                R.string.location_access_required,
                Snackbar.LENGTH_INDEFINITE,
                R.string.ok
            ) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            openSettingsForPermission()
        }
    }

    private fun openSettingsForPermission() {
        binding.incMapContent.mainContainer.showSnackbar(
            R.string.location_permission_not_available,
            Snackbar.LENGTH_INDEFINITE,
            R.string.settings
        ) {
            showSettingsForPermission = true
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts(PACKAGE, requireActivity().packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    private fun locationPermissionGranted() {
        updateLocationUI()
    }

    private fun isLocationPermissionGranted(): Boolean  {
        return (ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    companion object {
        private val TAG = MapFragment::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val KEYBOARD_DETECTION_DELAY = 100L
        private const val PACKAGE = "package"
    }
}
package com.aksatoskar.ycsplassignment.ui.main.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aksatoskar.ycsplassignment.R
import com.aksatoskar.ycsplassignment.databinding.FragmentMapBinding
import com.aksatoskar.ycsplassignment.model.Resource
import com.aksatoskar.ycsplassignment.ui.main.viewmodel.MainViewModel
import com.aksatoskar.ycsplassignment.util.hide
import com.aksatoskar.ycsplassignment.util.show
import com.aksatoskar.ycsplassignment.util.showSnackbar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private var mapCenterMarkerLatLng: LatLng? = LatLng(-33.8523341, 151.2106085)
    private var sheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null
    private var showSettingsForPermission = false

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }
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

    private fun initializeUI() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
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
    }

    private fun setListeners() {
        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheetContainer)
        sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        binding.fab.setOnClickListener {
            if (sheetBehavior?.state == BottomSheetBehavior.STATE_HIDDEN) {
                sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    onHideBottomSheet()
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    onExpandBottomSheet()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                //No handling required
            }
        })

        binding.bottomSheet.btnProceed.setOnClickListener {
            submitLocation()
        }
    }

    private fun onHideBottomSheet() {
        binding.incMapContent.mapMarker.setImageDrawable(ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_add
        ))
        binding.fab.setImageDrawable(ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_add
        ))
    }

    private fun onExpandBottomSheet() {
        binding.incMapContent.mapMarker.setImageDrawable(ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_marker
        ))
        binding.fab.setImageDrawable(ContextCompat.getDrawable(requireContext(),
            R.drawable.ic_close
        ))
    }

    private fun submitLocation() {
        val name = binding.bottomSheet.etPropertyName.text.toString()
        if (name.isBlank()) {
            return
        }

        mapCenterMarkerLatLng?.let {
            viewModel.insertLocation(it, name)
        }

        binding.bottomSheet.etPropertyName.setText("")
        sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (showSettingsForPermission) {
            showSettingsForPermission = false
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
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
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
                getDeviceLocation()
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(mapCenterMarkerLatLng, DEFAULT_ZOOM.toFloat()))
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
        mapCenterMarkerLatLng = map?.cameraPosition?.target
        binding.bottomSheet.etPropertyCoordinates.text = "${mapCenterMarkerLatLng?.latitude}, ${mapCenterMarkerLatLng?.longitude}"
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
            val uri = Uri.fromParts("package", requireActivity().packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    private fun locationPermissionGranted() {
        locationPermissionGranted = true
        updateLocationUI()
    }

    companion object {
        private val TAG = MapFragment::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }
}
package com.trien.dnmap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.trien.dnmap.Utils.readMarkersFromCSV
import com.trien.dnmap.Utils.readPolyLinePointsFromCSV
import com.trien.dnmap.Utils.resizeCommonAnnotation
import com.trien.dnmap.Utils.resizeMarker

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.text.DateFormat
import java.util.ArrayList
import java.util.Date

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener, ActivityCompat.OnRequestPermissionsResultCallback {


    private var mMap: GoogleMap? = null

    // Polyline instances
    private var mMutablePolylineBlue: Polyline? = null
    private var mMutablePolylineOrange: Polyline? = null
    private var mMutablePolylineViolet: Polyline? = null
    private var mMutablePolylineGreen: Polyline? = null
    private var mMutablePolylinePink: Polyline? = null

    // list of GroundOverlay objects
    private val mGroundOverlay = ArrayList<GroundOverlay>()

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Provides access to the Location Settings API.
     */
    private var mSettingsClient: SettingsClient? = null

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private var mLocationRequest: LocationRequest? = null

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    /**
     * Callback for Location events.
     */
    private var mLocationCallback: LocationCallback? = null

    /**
     * Represents a geographical location.
     */
    private var mLastLocation: Location? = null

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private var mRequestingLocationUpdates: Boolean? = null

    /**
     * Time when the location was updated represented as a String.
     */
    private var mLastUpdateTime: String? = null

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     */
    private var mPermissionDenied = false

    /* *************************************************************************************/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        // initialize mRequestingLocationUpdates and mLastUpdateTime
        mRequestingLocationUpdates = false
        mLastUpdateTime = ""

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState)

        // initialize FusedLocationProviderClient and SettingsClient object to invoke location settings
        // on app first time startup
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()
    }

    override fun onPause() {
        super.onPause()
        // Remove location updates to save battery.
        stopLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        // Remove location updates to save battery.
        stopLocationUpdates()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap?) {
        // initialize map object
        mMap = googleMap

        // set up location buttons and onclick events
        mMap!!.setOnMyLocationButtonClickListener(this)
        mMap!!.setOnMyLocationClickListener(this)
        enableMyLocation()

        // set up grey style for the map
        setMapStyle()

        // move camera to schoolies area
        moveCameraToSchooliesArea()

        // draw all polylines
        drawAllPolyLines()

        // set up custom info window for marker's onclick event
        val customInfoWindow = CustomInfoWindowGoogleMap(this)
        mMap!!.setInfoWindowAdapter(customInfoWindow)
        mMap!!.setOnMarkerClickListener { marker ->
            // if returning true, nothing will appear when a marker clicked
            // if returning false, default behavior will be executed (info window occurs)
            marker.snippet == null
        }

        // all all markers (bus stops) to the map
        addAllBusStopMarkers()

        // add all
        addAllAnnotationsAsMarkers()

        // add all direction arrows
        addAllDirectionArrowsAsGroundOverlay()
    }

    // method to add all direction arrows as ground overlay images
    private fun addAllDirectionArrowsAsGroundOverlay() {
        // blue arrows
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.586970, 138.597452), DIRECTION_ARROW_WIDTH)
                .bearing(250f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.579636, 138.598846), DIRECTION_ARROW_WIDTH)
                .bearing(100f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.571573, 138.592793), DIRECTION_ARROW_WIDTH)
                .bearing(55f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.574142, 138.604587), DIRECTION_ARROW_WIDTH)
                .bearing(315f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.563598, 138.601641), DIRECTION_ARROW_WIDTH)
                .bearing(170f)
                .clickable(false)))

        // orange arrows
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_orange))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.559907, 138.616066), DIRECTION_ARROW_WIDTH)
                .bearing(340f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_orange))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.556842, 138.625798), DIRECTION_ARROW_WIDTH)
                .bearing(265f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_orange))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.555459, 138.619425), DIRECTION_ARROW_WIDTH)
                .bearing(165f)
                .clickable(false)))

        // violet arrows
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_violet))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.549445, 138.622413), DIRECTION_ARROW_WIDTH)
                .bearing(305f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_violet))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.544262, 138.630482), DIRECTION_ARROW_WIDTH)
                .bearing(130f)
                .clickable(false)))

        // green arrows
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_green))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.533821, 138.662244), DIRECTION_ARROW_WIDTH)
                .bearing(170f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_green))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.530991, 138.667883), DIRECTION_ARROW_WIDTH)
                .bearing(350f)
                .clickable(false)))

        // pink arrows
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.517387, 138.685901), DIRECTION_ARROW_WIDTH)
                .bearing(300f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.513056, 138.698840), DIRECTION_ARROW_WIDTH)
                .bearing(170f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.507226, 138.736613), DIRECTION_ARROW_WIDTH)
                .bearing(355f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.508623, 138.747492), DIRECTION_ARROW_WIDTH)
                .bearing(175f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.504478, 138.760327), DIRECTION_ARROW_WIDTH)
                .bearing(280f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.497321, 138.771540), DIRECTION_ARROW_WIDTH)
                .bearing(355f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.508181, 138.777899), DIRECTION_ARROW_WIDTH)
                .bearing(100f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.512730, 138.773930), DIRECTION_ARROW_WIDTH)
                .bearing(280f)
                .clickable(false)))
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.506263, 138.770638), DIRECTION_ARROW_WIDTH)
                .bearing(175f)
                .clickable(false)))
    }

    // method to add all bus stop markers
    private fun addAllBusStopMarkers() {
        addBulkMarkers(MAIN_MARKER)
        addBulkMarkers(LINE_BLUE)
        addBulkMarkers(LINE_ORANGE)
        addBulkMarkers(LINE_VIOLET)
        addBulkMarkers(LINE_GREEN)
        addBulkMarkers(LINE_PINK)
    }

    // method to add all annotations on map as markers. This case the texts won't scale when we zoom the map
    private fun addAllAnnotationsAsMarkers() {
        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_whalers_inn)))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.5863, 138.59842))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_yilki_store)))
                .anchor(0f, 0f)
                .position(LatLng(-35.574789, 138.602354))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_victor_harbor_holiday_park)))
                .anchor(1f, 1f)
                .position(LatLng(-35.559881, 138.608045))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_beachfront_holiday_park)))
                .anchor(0f, 0f)
                .position(LatLng(-35.559180000000005, 138.61157))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_warland_reserve)))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.55678, 138.62361))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_adare_caravan_park)))
                .anchor(0f, 0f)
                .position(LatLng(-35.542840000000005, 138.63001))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_repco_bus_stop6)))
                .anchor(0.5f, 1f)
                .position(LatLng(-35.536411, 138.639775))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_bus_stop8)))
                .anchor(0.5f, 0f)
                .position(LatLng(-35.534787, 138.650920))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_port_elliot_bus_stop)))
                .anchor(0.5f, 1f)
                .position(LatLng(-35.530169, 138.681719))
        )
        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_port_elliot_holiday_park)))
                .anchor(0f, 0f)
                .position(LatLng(-35.530570000000004, 138.68959))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_middleton_store_stop12)))
                .anchor(0.3f, 0f)
                .position(LatLng(-35.510994, 138.703837))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_chapman_rd_stop13)))
                .anchor(0.3f, 1f)
                .position(LatLng(-35.509204, 138.721063))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_goolwa_camping_tourist_park)))
                .anchor(0.5f, 1f)
                .position(LatLng(-35.498173, 138.772636))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_goolwa_caltex)))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.499598, 138.78091))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_goolwa_stratco)))
                .anchor(0.5f, 0f)
                .position(LatLng(-35.504947, 138.779322))
        )
        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_beach_td)))
                .anchor(0.5f, 0f)
                .position(LatLng(-35.515146, 138.774872))
        )

        mMap!!.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(this, R.drawable.anno_bus_stop14)))
                .anchor(0.5f, 0f)
                .position(LatLng(-35.505026, 138.772299))
        )
    }

    // method to add all annotations on map as ground overlay. This case the texts will scale when we zoom the map (we don't want as of now)
    private fun addAllAnnotationsAsGroundOverlay() {
        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_whalers_inn))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.5863, 138.59842), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_yilki_store))
                .anchor(0f, 0f)
                .position(LatLng(-35.574789, 138.602354), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_victor_harbor_holiday_park))
                .anchor(1f, 1f)
                .position(LatLng(-35.559881, 138.608045), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_beachfront_holiday_park))
                .anchor(0f, 0f)
                .position(LatLng(-35.559180000000005, 138.61157), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_warland_reserve))
                .anchor(0f, 0.5f)
                .position(LatLng(-35.55678, 138.62361), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_adare_caravan_park))
                .anchor(0f, 0f)
                .position(LatLng(-35.542840000000005, 138.63001), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_repco_bus_stop6))
                .anchor(0.5f, 0f)
                .position(LatLng(-35.536411, 138.639775), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_bus_stop8))
                .anchor(0f, 0f)
                .position(LatLng(-35.534787, 138.650920), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_port_elliot_bus_stop))
                .anchor(1f, 0f)
                .position(LatLng(-35.530169, 138.681719), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_port_elliot_holiday_park))
                .anchor(0f, 0f)
                .position(LatLng(-35.530570000000004, 138.68959), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_middleton_store_stop12))
                .anchor(0.3f, 1f)
                .position(LatLng(-35.510994, 138.703837), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_chapman_rd_stop13))
                .anchor(0.3f, 0f)
                .position(LatLng(-35.509204, 138.721063), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_goolwa_camping_tourist_park))
                .anchor(0.5f, 0f)
                .position(LatLng(-35.498173, 138.772636), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_goolwa_caltex))
                .anchor(1f, 0.5f)
                .position(LatLng(-35.499598, 138.78091), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_goolwa_stratco))
                .anchor(0f, 0f)
                .position(LatLng(-35.504947, 138.779322), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_beach_td))
                .anchor(0.7f, 1f)
                .position(LatLng(-35.515146, 138.774872), 500f)
                .clickable(false)))

        mGroundOverlay.add(mMap!!.addGroundOverlay(GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_bus_stop14))
                .anchor(1f, 1f)
                .position(LatLng(-35.505026, 138.772299), 500f)
                .clickable(false)))
    }

    /**
     * Click handler for clamping to Schoolies bus area.
     */
    fun moveCameraToSchooliesArea() {
        mMap!!.setOnMapLoadedCallback {
            val bounds = LatLngBounds.Builder()
                    .include(BOUND1)
                    .include(BOUND2)
                    .build()
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))
        }
    }

    /**
     * Creates a [MapStyleOptions] object via loadRawResourceStyle() (or via the
     * constructor with a JSON String), then sets it on the [GoogleMap] instance,
     * via the setMapStyle() method.
     */
    private fun setMapStyle() {
        // Sets the grayscale style via raw resource JSON.
        val style = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_grayscale)
        mMap!!.setMapStyle(style)
    }

    // method to draw all poly lines
    private fun drawAllPolyLines() {

        mMutablePolylineBlue = mMap!!.addPolyline(PolylineOptions()
                .color(resources.getColor(R.color.colorPolyLineBlue))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(this, LINE_BLUE)))

        mMutablePolylineViolet = mMap!!.addPolyline(PolylineOptions()
                .color(resources.getColor(R.color.colorPolyLineViolet))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(this, LINE_VIOLET)))

        mMutablePolylineOrange = mMap!!.addPolyline(PolylineOptions()
                .color(resources.getColor(R.color.colorPolyLineOrange))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(this, LINE_ORANGE)))

        mMutablePolylineGreen = mMap!!.addPolyline(PolylineOptions()
                .color(resources.getColor(R.color.colorPolyLineGreen))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(this, LINE_GREEN)))

        mMutablePolylinePink = mMap!!.addPolyline(PolylineOptions()
                .color(resources.getColor(R.color.colorPolyLinePink))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(this, LINE_PINK)))
    }

    // helper method to add bulk markers to map as per line keyword
    private fun addBulkMarkers(lineKeyword: String) {

        val latLngList = readMarkersFromCSV(this, lineKeyword)

        var snippet = ""
        var resizedBitmap = resizeMarker(this, R.drawable.marker_blue_dark)

        when (lineKeyword) {
            LINE_BLUE -> {
                snippet = getString(R.string.snippet_zone1)
                resizedBitmap = resizeMarker(this, R.drawable.marker_blue_light)
            }
            LINE_ORANGE -> {
                snippet = getString(R.string.snippet_zone2)
                resizedBitmap = resizeMarker(this, R.drawable.marker_orange)
            }
            LINE_VIOLET -> {
                snippet = getString(R.string.snippet_zone3)
                resizedBitmap = resizeMarker(this, R.drawable.marker_violet)
            }
            LINE_GREEN -> {
                snippet = getString(R.string.snippet_zone4)
                resizedBitmap = resizeMarker(this, R.drawable.marker_green)
            }
            LINE_PINK -> {
                snippet = getString(R.string.snippet_zone5)
                resizedBitmap = resizeMarker(this, R.drawable.marker_pink)
            }
            MAIN_MARKER -> {
                snippet = getString(R.string.snippet_warland_reserve)
                resizedBitmap = resizeMarker(this, R.drawable.marker_blue_dark)
            }
        }

        for (latLng in latLngList) {
            val newMarker: Marker
            newMarker = mMap!!.addMarker(MarkerOptions()
                    .position(LatLng(latLng.latitude, latLng.longitude))
                    .title(" ")
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))
            //newMarker.showInfoWindow();
            //newMarker.setTag();
        }
    }

    /* *************************************************************************************
     *************************************************************************************
     *************************************************************************************
     *************************************************************************************
     * Location and permission supporting methods
     */

    /**
     * Stores activity data in the Bundle.
     */
    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates!!)
        savedInstanceState.putParcelable(KEY_LOCATION, mLastLocation)
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime)
        super.onSaveInstanceState(savedInstanceState)
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this@MapsActivity, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true)
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap!!.isMyLocationEnabled = true
        }
    }

    /**
     * Preserved method to get Last known location which can be used in User Preferences later.
     */

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES)
            }

            // Update the value of mLastLocation from the Bundle and move the camera to the last
            // known location.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mLastLocation
                // is not null.
                setLastLocation(savedInstanceState.getParcelable<Parcelable>(KEY_LOCATION) as Location)
                // Move camera to the last known location
                //                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING)
            }

        }
    }

    private fun setLastLocation(lastLocation: Location?) {

        mLastLocation = lastLocation

    }

    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                setLastLocation(locationResult!!.lastLocation)
                mLastUpdateTime = DateFormat.getTimeInstance().format(Date())

            }
        }
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`. These settings control
     * the accuracy of the current location. This app uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     *
     *
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     *
     *
     * These settings are appropriate for mapping applications that show real time location
     * updates.
     */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Uses a [com.google.android.gms.location.LocationSettingsRequest.Builder] to build
     * a [com.google.android.gms.location.LocationSettingsRequest] that is used for checking
     * if a device has the needed location settings.
     */
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(LOG_TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, View.OnClickListener {
                // Request permission
                ActivityCompat.requestPermissions(this@MapsActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            })
        } else {
            Log.i(LOG_TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this@MapsActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * actions after users choose to or not to change location settings
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.i(LOG_TAG, "User agreed to make required location settings changes.")
                Activity.RESULT_CANCELED -> {
                    Log.i(LOG_TAG, "User chose not to make required location settings changes.")
                    mRequestingLocationUpdates = false
                }
            }// Nothing to do. startLocationupdates() gets called in onResume again.
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(LOG_TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(LOG_TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates!!) {
                    Log.i(LOG_TAG, "Permission granted, updates requested, starting location updates")
                    startLocationUpdates()
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, View.OnClickListener {
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                })
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Enable the my location layer if the permission has been granted (disabled as we
                // use another icon in place of default my location btn)
                enableMyLocation()
            } else {
                // Display the missing permission error dialog when the fragments resume.
                mPermissionDenied = true
            }
        } else {
            return
        }
    }


    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient!!.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this) {
                    Log.i(LOG_TAG, "All location settings are satisfied.")


                    mFusedLocationClient!!.requestLocationUpdates(mLocationRequest,
                            mLocationCallback!!, Looper.myLooper())

                    try {
                        mFusedLocationClient!!.lastLocation
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful && task.result != null) {

                                        setLastLocation(task.result)
                                        // move camera to current location
                                        val latLng = LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude)
                                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
                                        // mMap.animateCamera(cameraUpdate);
                                        mMap!!.moveCamera(cameraUpdate)

                                    } else {
                                        Log.w(LOG_TAG, "Failed to get location.")
                                    }
                                }
                    } catch (unlikely: SecurityException) {
                        Log.e(LOG_TAG, "Lost location permission.$unlikely")
                    }
                }
                .addOnFailureListener(this) { e ->
                    val statusCode = (e as ApiException).statusCode
                    when (statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.i(LOG_TAG,
                                    "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(this@MapsActivity,
                                        REQUEST_CHECK_SETTINGS)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(LOG_TAG, "PendingIntent unable to execute request.")
                            }

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                            Log.e(LOG_TAG, errorMessage)
                            Toast.makeText(this@MapsActivity,
                                    errorMessage,
                                    Toast.LENGTH_LONG).show()
                            mRequestingLocationUpdates = false
                        }
                    }
                }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private fun stopLocationUpdates() {
        if ((!mRequestingLocationUpdates!!)!!) {
            Log.v(LOG_TAG, "stopLocationUpdates: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
                .addOnCompleteListener(this) { mRequestingLocationUpdates = false }
    }

    /**
     * Shows a [Snackbar].
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the OnAddSizzleButtonsClickListener item.
     * @param listener         The listener associated with the Snackbar OnAddSizzleButtonsClickListener.
     */
    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int,
                             listener: View.OnClickListener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }


    override fun onMyLocationButtonClick(): Boolean {

        // We receive location updates if permission has been granted
        if (checkPermissions()) {
            startLocationUpdates()
        } else if (!checkPermissions()) {
            requestPermissions()
        }

        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(location: Location) {

    }

    companion object {

        private val LOG_TAG = MapsActivity::class.java!!.getName()

        // bound values for camera focus on app start
        private val BOUND1 = LatLng(-35.595209, 138.585857)
        private val BOUND2 = LatLng(-35.494644, 138.805927)

        private val DIRECTION_ARROW_WIDTH = 400f

        /**
         * Code used in requesting runtime permissions.
         */
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

        // important: these keywords values must be exactly the same as ones in polylines.csv file in raw folder
        val MAIN_MARKER = "mainMarker"
        val LINE_BLUE = "lineBlue"
        val LINE_ORANGE = "lineOrange"
        val LINE_VIOLET = "lineViolet"
        val LINE_GREEN = "lineGreen"
        val LINE_PINK = "linePink"
        val MARKER = "marker"
        val LAT_LNG_POINT = "latLngPoint"

        /* *************************************************************************************
     * Below is all declarations for Location service
     */

        /**
         * Constant used in the location settings dialog.
         */
        private val REQUEST_CHECK_SETTINGS = 0x1

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

        // Keys for storing activity state in the Bundle.
        private val KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates"
        private val KEY_LOCATION = "location"
        private val KEY_LAST_UPDATED_TIME_STRING = "last-updated-transfusionTime-string"

        /**
         * Request code for location permission request.
         */
        private val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

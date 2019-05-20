package com.trien.dnmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
    
    private static final String LOG_TAG = MapsActivity.class.getName();

    // bound values for camera focus on app start
    private static final LatLng BOUND1 = new LatLng(-35.595209, 138.585857);
    private static final LatLng BOUND2 = new LatLng(-35.494644, 138.805927);

    private static final float DIRECTION_ARROW_WIDTH = 400f;

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // important: these keywords values must be exactly the same as ones in polylines.csv file in raw folder
    public static final String MAIN_MARKER = "mainMarker";
    public static final String LINE_BLUE = "lineBlue";
    public static final String LINE_ORANGE = "lineOrange";
    public static final String LINE_VIOLET = "lineViolet";
    public static final String LINE_GREEN = "lineGreen";
    public static final String LINE_PINK = "linePink";
    public static final String MARKER = "marker";
    public static final String LAT_LNG_POINT = "latLngPoint";

    private GoogleMap mMap;

    // Polyline instances
    private Polyline mMutablePolylineBlue;
    private Polyline mMutablePolylineOrange;
    private Polyline mMutablePolylineViolet;
    private Polyline mMutablePolylineGreen;
    private Polyline mMutablePolylinePink;

    // list of GroundOverlay objects
    private List<GroundOverlay> mGroundOverlay = new ArrayList<>();

    /* *************************************************************************************
     * Below is all declarations for Location service
     */

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-transfusionTime-string";

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mLastLocation;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    private String mLastUpdateTime;

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    /* *************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // initialize mRequestingLocationUpdates and mLastUpdateTime
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // initialize FusedLocationProviderClient and SettingsClient object to invoke location settings
        // on app first time startup
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove location updates to save battery.
        stopLocationUpdates();
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // initialize map object
        mMap = googleMap;

        // set up location buttons and onclick events
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();

        // set up grey style for the map
        setMapStyle();

        // move camera to schoolies area
        moveCameraToSchooliesArea();

        // draw all polylines
        drawAllPolyLines();

        // set up custom info window for marker's onclick event
        CustomInfoWindowGoogleMap customInfoWindow = new CustomInfoWindowGoogleMap(this);
        mMap.setInfoWindowAdapter(customInfoWindow);
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // if returning true, nothing will appear when a marker clicked
                // if returning false, default behavior will be executed (info window occurs)
                return marker.getSnippet() == null;
            }
        });

        // all all markers (bus stops) to the map
        addAllBusStopMarkers();

        // add all
        addAllAnnotationsAsMarkers();

        // add all direction arrows
        addAllDirectionArrowsAsGroundOverlay();
    }

    // method to add all direction arrows as ground overlay images
    private void addAllDirectionArrowsAsGroundOverlay() {
        // blue arrows
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.586970, 138.597452), DIRECTION_ARROW_WIDTH)
                .bearing(250)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.579636, 138.598846), DIRECTION_ARROW_WIDTH)
                .bearing(100)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.571573, 138.592793), DIRECTION_ARROW_WIDTH)
                .bearing(55)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.574142, 138.604587), DIRECTION_ARROW_WIDTH)
                .bearing(315)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_blue))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.563598, 138.601641), DIRECTION_ARROW_WIDTH)
                .bearing(170)
                .clickable(false)));

        // orange arrows
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_orange))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.559907, 138.616066), DIRECTION_ARROW_WIDTH)
                .bearing(340)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_orange))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.556842, 138.625798), DIRECTION_ARROW_WIDTH)
                .bearing(265)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_orange))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.555459, 138.619425), DIRECTION_ARROW_WIDTH)
                .bearing(165)
                .clickable(false)));

        // violet arrows
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_violet))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.549445, 138.622413), DIRECTION_ARROW_WIDTH)
                .bearing(305)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_violet))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.544262, 138.630482), DIRECTION_ARROW_WIDTH)
                .bearing(130)
                .clickable(false)));

        // green arrows
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_green))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.533821, 138.662244), DIRECTION_ARROW_WIDTH)
                .bearing(170)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_green))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.530991, 138.667883), DIRECTION_ARROW_WIDTH)
                .bearing(350)
                .clickable(false)));

        // pink arrows
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.517387, 138.685901), DIRECTION_ARROW_WIDTH)
                .bearing(300)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.513056, 138.698840), DIRECTION_ARROW_WIDTH)
                .bearing(170)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.507226, 138.736613), DIRECTION_ARROW_WIDTH)
                .bearing(355)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.508623, 138.747492), DIRECTION_ARROW_WIDTH)
                .bearing(175)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.504478, 138.760327), DIRECTION_ARROW_WIDTH)
                .bearing(280)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.497321, 138.771540), DIRECTION_ARROW_WIDTH)
                .bearing(355)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.508181, 138.777899), DIRECTION_ARROW_WIDTH)
                .bearing(100)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.512730, 138.773930), DIRECTION_ARROW_WIDTH)
                .bearing(280)
                .clickable(false)));
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.arrow_pink))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.506263, 138.770638), DIRECTION_ARROW_WIDTH)
                .bearing(175)
                .clickable(false)));
    }

    // method to add all bus stop markers
    private void addAllBusStopMarkers() {
        addBulkMarkers(MAIN_MARKER);
        addBulkMarkers(LINE_BLUE);
        addBulkMarkers(LINE_ORANGE);
        addBulkMarkers(LINE_VIOLET);
        addBulkMarkers(LINE_GREEN);
        addBulkMarkers(LINE_PINK);
    }

    // method to add all annotations on map as markers. This case the texts won't scale when we zoom the map
    private void addAllAnnotationsAsMarkers() {
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_whalers_inn)))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.5863, 138.59842))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_yilki_store)))
                .anchor(0, 0)
                .position(new LatLng(-35.574789, 138.602354))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_victor_harbor_holiday_park)))
                .anchor(1, 1)
                .position(new LatLng(-35.559881, 138.608045))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_beachfront_holiday_park)))
                .anchor(0, 0)
                .position(new LatLng(-35.559180000000005, 138.61157))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_warland_reserve)))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.55678, 138.62361))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_adare_caravan_park)))
                .anchor(0, 0)
                .position(new LatLng(-35.542840000000005, 138.63001))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_repco_bus_stop6)))
                .anchor(0.5f, 1)
                .position(new LatLng(-35.536411, 138.639775))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_bus_stop8)))
                .anchor(0.5f, 0)
                .position(new LatLng(-35.534787, 138.650920))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_port_elliot_bus_stop)))
                .anchor(0.5f, 1)
                .position(new LatLng(-35.530169, 138.681719))
        );
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_port_elliot_holiday_park)))
                .anchor(0, 0)
                .position(new LatLng(-35.530570000000004, 138.68959))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_middleton_store_stop12)))
                .anchor(0.3f, 0)
                .position(new LatLng(-35.510994, 138.703837))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_chapman_rd_stop13)))
                .anchor(0.3f, 1)
                .position(new LatLng(-35.509204, 138.721063))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_goolwa_camping_tourist_park)))
                .anchor(0.5f, 1)
                .position(new LatLng(-35.498173, 138.772636))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_goolwa_caltex)))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.499598, 138.78091))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_goolwa_stratco)))
                .anchor(0.5f, 0)
                .position(new LatLng(-35.504947, 138.779322))
        );
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_beach_td)))
                .anchor(0.5f, 0)
                .position(new LatLng(-35.515146, 138.774872))
        );

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resizeCommonAnnotation(R.drawable.anno_bus_stop14)))
                .anchor(0.5f, 0)
                .position(new LatLng(-35.505026, 138.772299))
        );
    }

    // method to add all annotations on map as ground overlay. This case the texts will scale when we zoom the map (we don't want as of now)
    private void addAllAnnotationsAsGroundOverlay() {
        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_whalers_inn))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.5863, 138.59842), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_yilki_store))
                .anchor(0, 0)
                .position(new LatLng(-35.574789, 138.602354), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_victor_harbor_holiday_park))
                .anchor(1, 1)
                .position(new LatLng(-35.559881, 138.608045), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_beachfront_holiday_park))
                .anchor(0, 0)
                .position(new LatLng(-35.559180000000005, 138.61157), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_warland_reserve))
                .anchor(0, 0.5f)
                .position(new LatLng(-35.55678, 138.62361), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_adare_caravan_park))
                .anchor(0, 0)
                .position(new LatLng(-35.542840000000005, 138.63001), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_repco_bus_stop6))
                .anchor(0.5f, 0)
                .position(new LatLng(-35.536411, 138.639775), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_bus_stop8))
                .anchor(0, 0)
                .position(new LatLng(-35.534787, 138.650920), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_port_elliot_bus_stop))
                .anchor(1, 0)
                .position(new LatLng(-35.530169, 138.681719), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_port_elliot_holiday_park))
                .anchor(0, 0)
                .position(new LatLng(-35.530570000000004, 138.68959), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_middleton_store_stop12))
                .anchor(0.3f, 1)
                .position(new LatLng(-35.510994, 138.703837), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_chapman_rd_stop13))
                .anchor(0.3f, 0)
                .position(new LatLng(-35.509204, 138.721063), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_goolwa_camping_tourist_park))
                .anchor(0.5f, 0)
                .position(new LatLng(-35.498173, 138.772636), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_goolwa_caltex))
                .anchor(1, 0.5f)
                .position(new LatLng(-35.499598, 138.78091), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_goolwa_stratco))
                .anchor(0, 0)
                .position(new LatLng(-35.504947, 138.779322), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_beach_td))
                .anchor(0.7f, 1)
                .position(new LatLng(-35.515146, 138.774872), 500f)
                .clickable(false)));

        mGroundOverlay.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.anno_bus_stop14))
                .anchor(1, 1)
                .position(new LatLng(-35.505026, 138.772299), 500f)
                .clickable(false)));
    }

    /**
     * Click handler for clamping to Schoolies bus area.
     */
    public void moveCameraToSchooliesArea() {
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                LatLngBounds bounds = new LatLngBounds.Builder()
                        .include(BOUND1)
                        .include(BOUND2)
                        .build();
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
            }
        });
    }

    /**
     * Creates a {@link MapStyleOptions} object via loadRawResourceStyle() (or via the
     * constructor with a JSON String), then sets it on the {@link GoogleMap} instance,
     * via the setMapStyle() method.
     */
    private void setMapStyle() {
        // Sets the grayscale style via raw resource JSON.
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_grayscale);
        mMap.setMapStyle(style);
    }

    // method to draw all poly lines
    private void drawAllPolyLines() {

        mMutablePolylineBlue = mMap.addPolyline(new PolylineOptions()
                .color(getResources().getColor(R.color.colorPolyLineBlue))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(LINE_BLUE)));

        mMutablePolylineViolet = mMap.addPolyline(new PolylineOptions()
                .color(getResources().getColor(R.color.colorPolyLineViolet))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(LINE_VIOLET)));

        mMutablePolylineOrange = mMap.addPolyline(new PolylineOptions()
                .color(getResources().getColor(R.color.colorPolyLineOrange))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(LINE_ORANGE)));

        mMutablePolylineGreen = mMap.addPolyline(new PolylineOptions()
                .color(getResources().getColor(R.color.colorPolyLineGreen))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(LINE_GREEN)));

        mMutablePolylinePink = mMap.addPolyline(new PolylineOptions()
                .color(getResources().getColor(R.color.colorPolyLinePink))
                .width(20f)
                .clickable(false)
                .addAll(readPolyLinePointsFromCSV(LINE_PINK)));
    }

    // helper method to read polyline points from CSV file
    private List<LatLng> readPolyLinePointsFromCSV(String lineKeyword) {
        InputStream is = getResources().openRawResource(R.raw.polylines);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8")));
        String line = "";
        List<LatLng> latLngList = new ArrayList<>();
        try {
            while ((line = reader.readLine()) != null) {
                // Split the line into different tokens (using the comma as a separator).
                String[] tokens = line.split(",");
                // only add the right latlng points to a desired line by color
                if (tokens[0].trim().equals(lineKeyword) && tokens[1].trim().equals(LAT_LNG_POINT)) {
                    latLngList.add(new LatLng(Double.parseDouble(tokens[2].trim()), Double.parseDouble(tokens[3].trim())));
                } else {
                    Log.d("trienPoly", " null");
                }
            }
        } catch (IOException e1) {
            Log.e("MainActivity", "Error" + line, e1);
            e1.printStackTrace();
        }

        for (LatLng lat : latLngList) {
            Log.d("trienPoly" + lineKeyword, lat.latitude + ", " + lat.longitude);
        }
        return latLngList;
    }

    // helper method to read markers lat lng values from CSV file
    private List<LatLng> readMarkersFromCSV(String lineKeyword) {
        InputStream is = getResources().openRawResource(R.raw.polylines);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8")));
        String line = "";
        List<LatLng> latLngList = new ArrayList<>();
        try {
            while ((line = reader.readLine()) != null) {
                // Split the line into different tokens (using the comma as a separator).
                String[] tokens = line.split(",");
                // only add the right latlng points to a desired line by color
                if (tokens[0].trim().equals(lineKeyword) && tokens[1].trim().equals(MARKER)) {
                    latLngList.add(new LatLng(Double.parseDouble(tokens[2].trim()), Double.parseDouble(tokens[3].trim())));
                    Log.d("trienMarker" + lineKeyword, tokens[2].trim() + tokens[3].trim());
                } else {
                    Log.d("trienMarker", " null");
                }
            }
        } catch (IOException e1) {
            Log.e("MainActivity", "Error" + line, e1);
            e1.printStackTrace();
        }

        return latLngList;
    }

    // helper method to add bulk markers to map as per line keyword
    private void addBulkMarkers(String lineKeyword) {

        List<LatLng> latLngList = readMarkersFromCSV(lineKeyword);

        String snippet = "";
        Bitmap resizedBitmap = resizeMarker(R.drawable.marker_blue_dark);

        switch (lineKeyword) {
            case LINE_BLUE:
                snippet = getString(R.string.snippet_zone1);
                resizedBitmap = resizeMarker(R.drawable.marker_blue_light);
                break;
            case LINE_ORANGE:
                snippet = getString(R.string.snippet_zone2);
                resizedBitmap = resizeMarker(R.drawable.marker_orange);
                break;
            case LINE_VIOLET:
                snippet = getString(R.string.snippet_zone3);
                resizedBitmap = resizeMarker(R.drawable.marker_violet);
                break;
            case LINE_GREEN:
                snippet = getString(R.string.snippet_zone4);
                resizedBitmap = resizeMarker(R.drawable.marker_green);
                break;
            case LINE_PINK:
                snippet = getString(R.string.snippet_zone5);
                resizedBitmap = resizeMarker(R.drawable.marker_pink);
                break;
            case MAIN_MARKER:
                snippet = getString(R.string.snippet_warland_reserve);
                resizedBitmap = resizeMarker(R.drawable.marker_blue_dark);
                break;
        }

        for (LatLng latLng : latLngList) {
            Marker newMarker;
            newMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latLng.latitude, latLng.longitude))
                    .title(" ")
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)));
            //newMarker.showInfoWindow();
            //newMarker.setTag();
        }
    }

    // bitmap resize tool
    private Bitmap resizeMarker(int drawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(drawable);
        Bitmap bitmap = bitmapDrawable.getBitmap();
        return Bitmap.createScaledBitmap(bitmap, 60, (bitmap.getHeight()* 60)/(bitmap.getWidth()), false);
    }

    // bitmap resize tool
    private Bitmap resizeCommonAnnotation(int drawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(drawable);
        Bitmap bitmap = bitmapDrawable.getBitmap();

        int newWidth = (int) (bitmapDrawable.getBitmap().getWidth()*0.15);
        int newHeight = (int) (bitmapDrawable.getBitmap().getHeight()*0.15);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }

    // bitmap resize tool
    private Bitmap resizeBitmap(int drawable, int width) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(drawable);
        Bitmap bitmap = bitmapDrawable.getBitmap();
        return Bitmap.createScaledBitmap(bitmap, width, (bitmap.getHeight()* 60)/(bitmap.getWidth()), false);
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mLastLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(MapsActivity.this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
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
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mLastLocation from the Bundle and move the camera to the last
            // known location.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mLastLocation
                // is not null.
                setLastLocation((Location) savedInstanceState.getParcelable(KEY_LOCATION));
                // Move camera to the last known location
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }

        }
    }

    private void setLastLocation(Location lastLocation) {

        mLastLocation = lastLocation;

    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                setLastLocation(locationResult.getLastLocation());
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

            }
        };
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This app uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(LOG_TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(LOG_TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * actions after users choose to or not to change location settings
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(LOG_TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(LOG_TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(LOG_TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(LOG_TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
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
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Enable the my location layer if the permission has been granted (disabled as we
                // use another icon in place of default my location btn)
                enableMyLocation();
            } else {
                // Display the missing permission error dialog when the fragments resume.
                mPermissionDenied = true;
            }
        } else {
            return;
        }
    }


    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(LOG_TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        try {
                            mFusedLocationClient.getLastLocation()
                                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Location> task) {
                                            if (task.isSuccessful() && task.getResult() != null) {

                                                setLastLocation(task.getResult());
                                                // move camera to current location
                                                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);
                                                // mMap.animateCamera(cameraUpdate);
                                                mMap.moveCamera(cameraUpdate);

                                            } else {
                                                Log.w(LOG_TAG, "Failed to get location.");
                                            }
                                        }
                                    });
                        } catch (SecurityException unlikely) {
                            Log.e(LOG_TAG, "Lost location permission." + unlikely);
                        }


                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(LOG_TAG,
                                        "Location settings are not satisfied. Attempting to upgrade " +
                                                "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapsActivity.this,
                                            REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(LOG_TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(LOG_TAG, errorMessage);
                                Toast.makeText(MapsActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }


                    }
                });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.v(LOG_TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the OnAddSizzleButtonsClickListener item.
     * @param listener         The listener associated with the Snackbar OnAddSizzleButtonsClickListener.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    @Override
    public boolean onMyLocationButtonClick() {

        // We receive location updates if permission has been granted
        if (checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {

    }
}

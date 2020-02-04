package com.pwestlake.walks

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.*
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Debug
import android.os.IBinder
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pwestlake.walks.activities.*
import com.pwestlake.walks.activities.WalkListFragment.OnListFragmentInteractionListener
import com.pwestlake.walks.bo.WalkMetaData
import com.pwestlake.walks.databinding.ActivityMainBinding
import com.pwestlake.walks.service.DaggerSettingsComponent
import com.pwestlake.walks.service.FileService
import com.pwestlake.walks.service.GPSService
import com.pwestlake.walks.service.SettingsService
import com.pwestlake.walks.utils.Trkpt
import com.pwestlake.walks.utils.readGPXAsLatLng
import kotlinx.coroutines.*
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject


class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnListFragmentInteractionListener {
    @Inject lateinit var settingsService: SettingsService
    @Inject lateinit var fileService: FileService
    private lateinit var gpsService: GPSService
    private var gpsServiceBound: Boolean = false

    private val EDIT_INTENT_CODE = 1
    private lateinit var mapView: MapView
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val startStopStateMachine = StartStopStateMachine()
    private lateinit var walkListFragment: WalkListFragment
    private lateinit var job: Job
    private lateinit var currentMetaData: WalkMetaData

    var deleteEnabled = ObservableField<Boolean>()
    var editEnabled = ObservableField<Boolean>()
    var altitude = ObservableField<String>()
    var elapsedTime = ObservableField<String>()
    var secondsCount = 0L
    var sectionCount = 0L
    var sectionStart = Date()
    var distance = ObservableField<String>()
    var speed = ObservableField<String>()

    private lateinit var deleteDialog: AlertDialog

    val route = ArrayList<Trkpt>()
    private var currentPolyline: Polyline? = null

    private var currentItem = ""
    private val colours = intArrayOf(0xFFB71C1C.toInt(), 0xFF263238.toInt(), 0xFF880E4F.toInt())

    private val locationBroadcastReceiver: BroadcastReceiver = LocationBroadcastReceiver()


    class LocationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            (context as MainActivity).updateRoute(intent)
        }
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as GPSService.LocalBinder
            gpsService = binder.getService()
            gpsServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            gpsServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = Color.TRANSPARENT
        }


        Intent(this, GPSService::class.java).also { intent ->
            startForegroundService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        val filter = IntentFilter("com.pwestlake.action.LOCATION_UPDATE")
        registerReceiver(locationBroadcastReceiver, filter)

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_main)
        binding.startStopStateMachine = startStopStateMachine
        binding.mainActivity = this

        DaggerSettingsComponent.create().inject(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val bottomSheet: View = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = from(bottomSheet)
        bottomSheetBehavior.setBottomSheetCallback(object: BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, value: Float) {
                // Ignore
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val expandView: ImageView = findViewById(R.id.expandView)
                val collapseView: ImageView = findViewById(R.id.collapseView)
                val toolbar = findViewById<Toolbar>(R.id.toolbar)

                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        collapseView.isVisible = true
                        expandView.isVisible = false
                        toolbar.inflateMenu(R.menu.actions)
                        findViewById<FloatingActionButton>(R.id.stopFloatingActionButton).hide()
                        when(startStopStateMachine.currentState) {
                            paused, stopped -> {
                                findViewById<FloatingActionButton>(R.id.startFloatingActionButton).hide()
                            }
                            running -> {
                                findViewById<FloatingActionButton>(R.id.pauseFloatingActionButton).hide()
                            }
                        }

                        val panel = findViewById<View>(R.id.detailsPanel)
                        panel.animate().alpha(0.0F)
                            .setDuration(300).setListener(object:
                            AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                panel.visibility = View.GONE
                            }
                        })


                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        val panel = findViewById<View>(R.id.detailsPanel)
                        panel.apply {
                            alpha = 1.0F
                            visibility = View.VISIBLE }

                    }
                    else -> {
                        collapseView.isVisible = false
                        expandView.isVisible = true

                        toolbar.menu.clear()

                        when(startStopStateMachine.currentState) {
                            paused -> {
                                findViewById<FloatingActionButton>(R.id.startFloatingActionButton).show()
                                findViewById<FloatingActionButton>(R.id.stopFloatingActionButton).show()
                            }
                            running -> {
                                findViewById<FloatingActionButton>(R.id.pauseFloatingActionButton).show()
                                findViewById<FloatingActionButton>(R.id.stopFloatingActionButton).show()
                            }
                            stopped -> {
                                findViewById<FloatingActionButton>(R.id.startFloatingActionButton).show()
                            }
                        }

                    }
                }
            }

        })

        walkListFragment = supportFragmentManager.findFragmentById(R.id.walkListFragment) as WalkListFragment

        deleteDialog = this.let {
            val builder = AlertDialog.Builder(it)

            builder.apply {
                setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { _, _ ->
                        deleteSelected()
                    })
                setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, _ ->
                        dialog.cancel()
                    })
            }

            builder.setTitle(R.string.delete_title)

            // Create the AlertDialog
            builder.create()
        }

        elapsedTime.set(formatTime(0))
        distance.set("0")
        speed.set("0")
    }

    fun calculatePathDistance(path: List<Trkpt>): Double {
        var dist = 0.0

        if (path.size > 1) {
            for (i in 0 until (path.size - 1)) {
                val a = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = path[i].lat
                    longitude = path[i].lon
                }

                val b = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = path[i + 1].lat
                    longitude = path[i + 1].lon
                }

                dist += a.distanceTo(b)
            }
        }

        return dist
    }

    fun calculatePathSpeed(path: List<Trkpt>): Double {
        var avgSpeed = 0.0
        var dist = 0.0
        var seconds = 0L

        if (path.size > 1) {
            for (i in 0 until (path.size - 1)) {
                val a = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = path[i].lat
                    longitude = path[i].lon
                }

                val b = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = path[i + 1].lat
                    longitude = path[i + 1].lon
                }

                dist += a.distanceTo(b)
                seconds += path[i + 1].time.time - path[i].time.time
            }

            if (seconds > 0) {
                avgSpeed = dist / seconds // m/ms
            }
        }

        return avgSpeed
    }

    fun updateRoute(intent: Intent?): Unit {
        val trkpt = intent?.getParcelableExtra<Trkpt>("trkpt")

        if (trkpt != null) {
            altitude.set(String.format("%.2f", trkpt.elevation) ?: "0")

            route.add(trkpt)
            distance.set(String.format("%.2f", (calculatePathDistance(route) / 1609.34)))

            val avgSpeed = (calculatePathSpeed(route) * 3600000) / 1609.34 // mph
            speed.set(String.format("%.2f", avgSpeed))

            if (currentPolyline == null) {
                currentPolyline = addRouteToMap(route.stream()
                    .map{it -> LatLng(it.lat, it.lon)}
                    .collect(Collectors.toList()), 0xff01579b.toInt())
            } else {
                val points = currentPolyline?.points
                points?.add(LatLng(trkpt.lat, trkpt.lon))
                currentPolyline?.points = points
            }

            val boundsBuilder = LatLngBounds.builder()
            for (point in route) {
                boundsBuilder.include(LatLng(point.lat, point.lon))
            }

            val viewBounds = gMap?.projection?.visibleRegion?.latLngBounds
            boundsBuilder.include(viewBounds?.northeast)
            boundsBuilder.include(viewBounds?.southwest)
            gMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 0));
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        gMap = googleMap
        gMap?.setPadding(0, 50, 0, 0)
        gMap?.isMyLocationEnabled = true

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val here = LatLng(location?.latitude ?: -34.0, location?.longitude ?: 151.0)
            gMap?.addMarker(MarkerOptions().position(here).title("Current location"))
            gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 15F))

            altitude.set(String.format("%.2f", location?.altitude) ?: "0")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }

            else -> {
                // invalid
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.clearFocus()
    }

    override fun onDestroy() {
        super.onDestroy()

        unbindService(connection)
        gpsServiceBound = false
    }


    fun onBottomSheetClose(view: View) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onListFragmentInteraction(item: WalkMetaData?) {
    }

    override fun itemSelectionChanged(items: List<WalkMetaData>) {
        gMap?.clear()

        // Redraw routes for checked items
        for ((index,item) in items.withIndex()) {
            val gpx = fileService.getGPXFile(applicationContext, item.id)
            if (gpx != null) {
                val route = readGPXAsLatLng(gpx)

                var colour = 0xff000000.toInt()
                if (index < colours.size) {
                    colour = colours[index]
                }
                addRouteToMap(route, colour)
            }
        }

    }

    private fun addRouteToMap(route: List<LatLng>, colour: Int): Polyline? {
        val polyLineOptions = PolylineOptions()
        polyLineOptions.color(colour)
        polyLineOptions.addAll(route)

        return gMap?.addPolyline(polyLineOptions)
    }

    override fun onOneListItemChecked() {
        deleteEnabled.set(true)
        editEnabled.set(true)
    }

    override fun onMoreThanOneListItemChecked() {
        editEnabled.set(false)
    }

    override fun onListItemChecksCleared() {
        deleteEnabled.set(false)
        editEnabled.set(false)
    }

    fun runClick(view: View) {
        if (startStopStateMachine.currentState != paused) {
            currentMetaData = WalkMetaData("", "unnamed", Date())
            currentItem = fileService.createWalkFile(this.applicationContext, currentMetaData)

            walkListFragment.addItem(currentMetaData)

            gpsService.path.clear()
            route.clear()
            gMap?.clear()
            currentPolyline = null

            secondsCount = 0L
        }

        gpsService.startTracking()

        startStopStateMachine.nextState(com.pwestlake.walks.activities.run)

        sectionStart = Date()
        sectionCount = secondsCount
        job = GlobalScope.launch(Dispatchers.Main) {
            while(isActive) {
                delay(1000)
                secondsCount = sectionCount + (Date().time - sectionStart.time) / 1000

                val time = formatTime(secondsCount)
                elapsedTime.set(time)
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val seconds = seconds % 60
        val time = String.format("%1$02d:%2$02d:%3$02d", hours, minutes, seconds)
        return time
    }

    fun pauseClick(view: View) {
        gpsService.pauseTracking()
        startStopStateMachine.nextState(com.pwestlake.walks.activities.pause)

        job.cancel()
    }

    fun stopClick(view: View) {
        startStopStateMachine.nextState(com.pwestlake.walks.activities.stop)

        fileService.writeGPXFile(view.context, currentItem, gpsService.path)
        currentMetaData.id = currentItem
        currentMetaData.distance = calculatePathDistance(route)
        currentMetaData.speed = calculatePathSpeed(route)
        currentMetaData.duration = secondsCount
        fileService.updateItem(view.context, currentMetaData)
        walkListFragment.updateItem(currentMetaData)

        gpsService.stopTracking()

        job.cancel()
    }

    fun deleteClick(view: View) {
        val selectedItems = walkListFragment.getSelectedItems()
        val items = selectedItems.size
        val itemsString = if (items == 1) " item" else " items"
        deleteDialog.setMessage("Confirm delete of " + items + itemsString)
        deleteDialog.show()
    }

    fun editClick(view: View) {
        val intent = Intent(this, Edit::class.java)
        val bundle = Bundle()
        bundle.putParcelable("item", walkListFragment.getSelectedItems().first())
        intent.putExtras(bundle)
        startActivityForResult(intent, EDIT_INTENT_CODE)
    }

    fun deleteSelected() {
        walkListFragment.deleteSelectedItems()
        deleteEnabled.set(false)
        editEnabled.set(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == 0 && requestCode == EDIT_INTENT_CODE) {
            if (data != null && data.hasExtra("item")) {
                val item: WalkMetaData? = data.getExtras()?.getParcelable<WalkMetaData>("item")

                if (item != null) {
                    walkListFragment.updateItem(item)
                    fileService.updateItem(this.applicationContext, item)
                    editEnabled.set(false)
                    deleteEnabled.set(false)
                }
            }
        }
    }
}



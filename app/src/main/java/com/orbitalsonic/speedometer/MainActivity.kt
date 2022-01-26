package com.orbitalsonic.speedometer

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.anastr.speedviewlib.AwesomeSpeedometer
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.orbitalsonic.speedometer.Constants.LOCATION_PERMISSIONS_REQUEST_CODE
import com.orbitalsonic.speedometer.Constants.newLatLong
import com.orbitalsonic.speedometer.Constants.oldLatLong
import com.orbitalsonic.speedometer.databinding.ActivityMainBinding
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var speedometer: AwesomeSpeedometer
    private var isFirstLocation = true

    private var locationForegroundService: LocationForegroundService? = null
    private lateinit var foregroundBroadcastReceiver: ForegroundBroadcastReceiver
    private lateinit var  serviceIntent: Intent

    private var foregroundServiceBound = false

    private val foregroundServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationForegroundService.LocalBinder
            locationForegroundService = binder.service
            foregroundServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationForegroundService = null
            foregroundServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        speedometer = findViewById(R.id.speedView)

        foregroundBroadcastReceiver = ForegroundBroadcastReceiver()
        serviceIntent = Intent(this, LocationForegroundService::class.java)

        binding.btnStart.setOnClickListener {
            if (locationPermissionApproved()) {
                locationForegroundService?.startLocation()
            } else {
                requestLocationPermissions()
            }
        }

    }

    private fun  settingViews(mLocationString:String){

        val splitLocSW = mLocationString.split(",").toTypedArray()

        val lat:Double = splitLocSW[0].toDouble()
        val lng:Double = splitLocSW[1].toDouble()

        if (isFirstLocation){
            isFirstLocation = false
            oldLatLong = LatLng(lat, lng)
            newLatLong = LatLng(lat, lng)
        }else{
            oldLatLong = newLatLong
            newLatLong = LatLng(lat, lng)
        }

        Log.i("ServiceTesting","oldLatLong: Lat${(oldLatLong.latitude)}, Lon${(oldLatLong.longitude)}")
        Log.i("ServiceTesting","newLatLong: Lat${(newLatLong.latitude)}, Lon${(newLatLong.longitude)}")

        setSpeedMeter(oldLatLong.latitude, oldLatLong.longitude, newLatLong.latitude, newLatLong.longitude)

    }


    private fun setSpeedMeter( lat1: Double,
                               lng1: Double,
                               lat2: Double,
                               lng2: Double){

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lng2 - lng1)
        val a = (sin(dLat / 2) * sin(dLat / 2)
                + (cos(Math.toRadians(lat1))
                * cos(Math.toRadians(lat2)) * sin(dLon / 2)
                * sin(dLon / 2)))
        val c = 2 * asin(sqrt(a))
        val distanceInMeters = (6371000 * c).roundToLong()

        // meter per second 5 is in second
        val speedMPS:Float = (distanceInMeters/5).toFloat()
        val speedKMPH:Float = (speedMPS * 3.6).toFloat()
        speedometer.speedTo(50F)

        Log.i("ServiceTesting","Speed:$speedKMPH")
    }

    override fun onStart() {
        super.onStart()
        bindService(serviceIntent, foregroundServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (foregroundServiceBound) {
            unbindService(foregroundServiceConnection)
            foregroundServiceBound = false
        }

        super.onStop()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundBroadcastReceiver
        )
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundBroadcastReceiver,
            IntentFilter(
                Constants.ACTION_LOCATION_FOREGROUND_BROADCAST
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
//        setRunningTimer(this,false)
        locationForegroundService?.destroyService()
    }

    private inner class ForegroundBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val mLocation = intent.getStringExtra(Constants.EXTRA_LOCATION)

            if (mLocation != null) {
                settingViews(mLocation)
            }
        }
    }

    private fun locationPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestLocationPermissions() {
        val provideRationale = locationPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(binding.mainActivity,
                "Location permission needed for tracking the path",
                Snackbar.LENGTH_LONG
            ).setAction("OK") {
                // Request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSIONS_REQUEST_CODE
                )
            }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSIONS_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d("PermissionTag", "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->{
                    // Permission was granted.
                    Log.d("PermissionTag", "Permission was granted")
                    locationForegroundService?.startLocation()
                }

                else -> {
                    // Permission denied.

                    Snackbar.make(binding.mainActivity,
                        "Permission was denied, but is needed for tracking path",
                        Snackbar.LENGTH_LONG
                    ).setAction("Settings") {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID,
                            null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }.show()
                }
            }
        }
    }

}
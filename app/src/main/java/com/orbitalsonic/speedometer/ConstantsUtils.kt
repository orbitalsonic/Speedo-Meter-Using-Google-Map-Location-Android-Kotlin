package com.orbitalsonic.speedometer

import com.google.android.gms.maps.model.LatLng

object ConstantsUtils {

    const val LOCATION_PERMISSIONS_REQUEST_CODE = 34

    const val ACTION_LOCATION_FOREGROUND_BROADCAST =
        "action.FOREGROUND_LOCATION_BROADCAST"

    const val EXTRA_LOCATION = "extra.LOCATION"

    // location updates interval - 10sec
    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000


    var oldLatLong:LatLng = LatLng(0.0, 0.0)
    var newLatLong:LatLng = LatLng(0.0, 0.0)

}
package com.sohan.comparo.service

import android.content.Context
import android.content.Intent
import android.util.Log

class NavigationController(private val context: Context) {

    companion object {
        private const val TAG = "NavigationController"
        const val PKG_SWIGGY = "in.swiggy.android"
        const val PKG_ZEPTO = "com.zeptonow.consumer"
        const val PKG_BLINKIT = "com.blinkit.user" // or cn.blinkit.user (check actual pkg)
    }

    fun launchApp(packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Log.d(TAG, "Launched $packageName")
            } else {
                Log.e(TAG, "App not found: $packageName")
                // TODO: Notify user to install app via overlay?
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
        }
    }
}

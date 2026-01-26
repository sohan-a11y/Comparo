package com.sohan.comparo

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.sohan.comparo.service.ComparoAccessibilityService
import com.sohan.comparo.ui.ComparoApp
import com.sohan.comparo.ui.PermissionType

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var isOverlayGranted by remember { mutableStateOf(checkOverlayPermission(this)) }
            var isAccessibilityGranted by remember { mutableStateOf(checkAccessibilityPermission(this)) }
            
            // Re-check permissions on resume (simple way is using LaunchedEffect with a key that updates on resume, 
            // but for MVP we can check on every recomposition or loop, or just rely on user clicking "Enable" again to re-check)
            DisposableEffect(LocalContext.current) {
               val lifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
                   if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                       isOverlayGranted = checkOverlayPermission(this@MainActivity)
                       isAccessibilityGranted = checkAccessibilityPermission(this@MainActivity)
                   }
               }
               lifecycle.addObserver(lifecycleObserver)
               onDispose {
                   lifecycle.removeObserver(lifecycleObserver)
               }
            }

            ComparoApp(
                onPermissionGrant = { type ->
                    when (type) {
                        PermissionType.OVERLAY -> requestOverlayPermission()
                        PermissionType.ACCESSIBILITY -> requestAccessibilityPermission()
                    }
                },
                isOverlayGranted = isOverlayGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                onStartService = {
                    // Service is started by system when accessibility is enabled. 
                    // This button acts as a confirmation/launch to home.
                }
            )
        }
    }

    private fun checkOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun checkAccessibilityPermission(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}

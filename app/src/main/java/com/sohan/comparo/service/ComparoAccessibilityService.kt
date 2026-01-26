package com.sohan.comparo.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class ComparoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ComparoAccessibility"
    }

    private lateinit var overlayManager: OverlayManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Comparo Accessibility Service Connected")
        overlayManager = OverlayManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            if (isTargetApp(packageName)) {
                Log.d(TAG, "Target app detected: $packageName")
                overlayManager.showOverlay()
            }
        }
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            // Limit parsing frequency to avoid lag?
            // For MVP, just parse
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val data = ScreenParser.parseScreen(rootNode)
                if (data.detectedTotal > 0) {
                    overlayManager.updateData("Total: ₹${data.detectedTotal}")
                }
            }
        }
    }
    
    private fun isTargetApp(pkg: String): Boolean {
        return pkg.contains("swiggy") || pkg.contains("zepto") || pkg.contains("blinkit") || pkg.contains("grofers")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Comparo Service Interrupted")
    }
}

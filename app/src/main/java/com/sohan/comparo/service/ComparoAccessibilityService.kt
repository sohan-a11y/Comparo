package com.sohan.comparo.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ComparoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ComparoAccessibility"
    }

    private lateinit var overlayManager: OverlayManager
    private lateinit var navigationController: NavigationController
    private lateinit var automationEngine: AutomationEngine
    
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Comparo Accessibility Service Connected")
        overlayManager = OverlayManager(this)
        navigationController = NavigationController(this)
        automationEngine = AutomationEngine(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            if (isTargetApp(packageName)) {
                Log.d(TAG, "Target app detected: $packageName")
                overlayManager.showOverlay {
                    // On Compare Clicked
                    Log.d(TAG, "Compare clicked! Starting automation...")
                    // Example: Launch Zepto
                    navigationController.launchApp(NavigationController.PKG_ZEPTO)
                    // We will need to wait for app load before automating search...
                }
            }
        }
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            val rootNode = rootInActiveWindow ?: return
            
            // 1. Fast Local Parse
            val localData = ScreenParser.parseScreen(rootNode)
            if (localData.detectedTotal > 0) {
                 overlayManager.updateData("Total: ₹${localData.detectedTotal}")
            }
            
            // 2. Slow AI Parse (if Key exists)
            // Debounce or rate limit could be added here
            if (com.sohan.comparo.network.GroqApiClient.apiKey.isNotEmpty()) {
                serviceScope.launch {
                    val aiData = ScreenParser.parseWithAi(localData.allText, localData.detectedTotal)
                    if (aiData.isAiResult) {
                        overlayManager.updateData("AI Total: ₹${aiData.detectedTotal} ✨")
                    }
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

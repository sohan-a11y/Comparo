package com.sohan.comparo.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.sohan.comparo.data.ComparisonRepository
import com.sohan.comparo.data.ScannedItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class ComparoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ComparoAccessibility"
    }

    private lateinit var overlayManager: OverlayManager
    private lateinit var navigationController: NavigationController
    private lateinit var automationEngine: AutomationEngine
    
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    private enum class AutomationState {
        IDLE,
        READING_SOURCE,
        NAVIGATING_TO_COMPETITOR,
        SEARCHING_PRODUCT,
        TYPING_PRODUCT,
        READING_COMPETITOR_PRICE,
        FINISHED
    }

    private var currentState = AutomationState.IDLE
    private var sourceProduct: String = ""
    private var currentCompetitorPkg: String = ""

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
        
        // 1. Always check for window state changes to detect App Launches
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            if (currentState == AutomationState.NAVIGATING_TO_COMPETITOR) {
                if (packageName == currentCompetitorPkg) {
                    Log.d(TAG, "Arrived at competitor: $packageName")
                    currentState = AutomationState.SEARCHING_PRODUCT
                    overlayManager.updateData("Searching '$sourceProduct'...")
                    // Give app time to load UI
                    serviceScope.launch {
                        delay(3000) 
                        performSearchInCompetitor()
                    }
                }
            }
            
            if (isTargetApp(packageName)) {
                Log.d(TAG, "Target app detected: $packageName")
                overlayManager.showOverlay {
                    // On Compare Clicked
                    Log.d(TAG, "Compare clicked! Starting automation...")
                    // Example: Launch Zepto
                    startAutomation("Milk") // Hardcoded product for MVP demo
                }
            }
        }
        
        // 2. Content Change (Reading Data)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             val rootNode = rootInActiveWindow ?: return
             
             // Passive Reading (Comparo Engine)
             val data = ScreenParser.parseScreen(rootNode)
             if (data.detectedTotal > 0) {
                 // Save to Repository
                 val appName = getAppName(event.packageName?.toString())
                 ComparisonRepository.addItem(ScannedItem(appName, "Cart Total", data.detectedTotal, true))
                 
                 // Push to UI
                 overlayManager.updateItems(ComparisonRepository.scannedItems.value)
             }
             
             // AI Parse (Optional)
             if (com.sohan.comparo.network.GroqApiClient.apiKey.isNotEmpty()) {
                // serviceScope.launch { ... } // Re-enable if needed
             }
        }
    }
    
    private fun startAutomation(product: String) {
        currentState = AutomationState.NAVIGATING_TO_COMPETITOR
        sourceProduct = product
        currentCompetitorPkg = NavigationController.PKG_ZEPTO
        
        ComparisonRepository.setStatus("Switching to Zepto...")
        navigationController.launchApp(currentCompetitorPkg)
    }

    private suspend fun performSearchInCompetitor() {
        val root = rootInActiveWindow ?: return
        
        // Find Search Button
        val searchNode = automationEngine.findSearchButton(root)
        if (searchNode != null) {
            ComparisonRepository.setStatus("Clicking Search...")
            automationEngine.clickNode(searchNode)
            
            delay(2000) // Wait for Search Bar to open
            
            // Find Edit Text (re-fetch root)
            val root2 = rootInActiveWindow
            val editNode = automationEngine.findNodeByClass(root2, "android.widget.EditText")
            if (editNode != null) {
                ComparisonRepository.setStatus("Typing '$sourceProduct'...")
                automationEngine.typeText(editNode, sourceProduct)
            } else {
                Log.e(TAG, "Could not find Search Bar EditText")
            }
            
        } else {
            Log.e(TAG, "Could not find Search Button")
            ComparisonRepository.setStatus("Search button not found")
        }
    }

    private fun getAppName(pkg: String?): String {
        return when {
            pkg?.contains("swiggy") == true -> "Swiggy"
            pkg?.contains("zepto") == true -> "Zepto"
            pkg?.contains("blinkit") == true -> "Blinkit"
            else -> "Unknown"
        }
    }
    
    private fun isTargetApp(pkg: String): Boolean {
        return pkg.contains("swiggy") || pkg.contains("zepto") || pkg.contains("blinkit") || pkg.contains("grofers")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Comparo Service Interrupted")
    }
}

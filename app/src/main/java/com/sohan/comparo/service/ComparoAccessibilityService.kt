package com.sohan.comparo.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.sohan.comparo.data.ComparisonRepository
import com.sohan.comparo.data.ScannedItem
import kotlinx.coroutines.delay
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
        
    private enum class AutomationState {
        IDLE,
        READING_SOURCE, // Reading "Milk" from Swiggy
        NAVIGATING_TO_COMPETITOR, // Opening Zepto
        SEARCHING_PRODUCT, // Clicking Search in Zepto
        TYPING_PRODUCT, // Typing "Milk"
        READING_COMPETITOR_PRICE, // Reading Zepto Price
        FINISHED
    }

    private var currentState = AutomationState.IDLE
    private var sourceProduct: String = ""
    private var currentCompetitorPkg: String = ""

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
               // Show overlay if not already (or update it)
               // overlayManager.showOverlay { ... } 
               // (Handled by manual trigger currently)
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
        }
    }
    
    private fun startAutomation(product: String) {
        currentState = AutomationState.NAVIGATING_TO_COMPETITOR
        sourceProduct = product
        currentCompetitorPkg = NavigationController.PKG_ZEPTO // Hardcoded for Demo
        
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
            
            // Find Edit Text
            val root2 = rootInActiveWindow
            val editNode = automationEngine.findNodeByClass(root2, "android.widget.EditText")
            if (editNode != null) {
                ComparisonRepository.setStatus("Typing '$sourceProduct'...")
                automationEngine.typeText(editNode, sourceProduct)
                // Need to press Enter? 
            } else {
                Log.e(TAG, "Could not find Search Bar EditText")
            }
            
        } else {
            Log.e(TAG, "Could not find Search Button")
            ComparisonRepository.setStatus("Search button not found. Using AI fallback...")
            // AI Fallback logic would go here
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

package com.sohan.comparo.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sohan.comparo.ui.OverlayContent

class OverlayManager(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    
    // Lifecycle components needed for Compose
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStore = ViewModelStore()

    private val _overlayText = androidx.compose.runtime.mutableStateOf("Scanning...")
    private val _scannedItemsState = androidx.compose.runtime.mutableStateOf<List<com.sohan.comparo.data.ScannedItem>>(emptyList())

    // We need to observe the repository flow. Since this is not a Composable, we launch a collector job.
    // In a real app we'd use a proper View-Model or Service-Scope binding.
    // For this MVP, we will rely on the Service pushing updates or simple polling, 
    // OR we trigger a refresh when data changes.
    // Better: let the Service update us.
    
    fun updateItems(items: List<com.sohan.comparo.data.ScannedItem>) {
        _scannedItemsState.value = items
    }

    fun showOverlay(onCompareTrigger: () -> Unit) {
        if (overlayView != null) return // Already showing

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        savedStateRegistryController.performRestore(null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        overlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayManager)
            setViewTreeViewModelStoreOwner(this@OverlayManager)
            setViewTreeSavedStateRegistryOwner(this@OverlayManager)
            
            setContent {
                OverlayContent(
                    textState = _overlayText,
                    scannedItems = _scannedItemsState.value,
                    onCompareClick = onCompareTrigger
                )
            }
        }
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        windowManager.addView(overlayView, params)
    }

    fun hideOverlay() {
        if (overlayView != null) {
             lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
             windowManager.removeView(overlayView)
             overlayView = null
        }
    }
    
    fun updateData(data: String) {
        _overlayText.value = data
    }

    // LifestyleOwner implementation
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    // SavedStateRegistryOwner implementation
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // ViewModelStoreOwner implementation
    override val viewModelStore: ViewModelStore get() = viewModelStore
}

// Interface to fix LifecycleOwner generic constraint issue if any
interface LifecycleOwner : androidx.lifecycle.LifecycleOwner

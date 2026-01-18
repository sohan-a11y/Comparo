package com.comparo.app

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var platformTabs: TabLayout
    private lateinit var urlEditText: EditText
    private lateinit var goButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var refreshButton: ImageButton
    private lateinit var homeButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: PlatformPagerAdapter

    private val platforms = arrayOf("Zepto", "Swiggy Instamart", "Blinkit")
    private val platformUrls = arrayOf(
        "https://www.zepto.com",
        "https://www.swiggy.com/instamart",
        "https://blinkit.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupViewPager()
        setupListeners()
        setupBackPressHandler()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        platformTabs = findViewById(R.id.platformTabs)
        urlEditText = findViewById(R.id.urlEditText)
        goButton = findViewById(R.id.goButton)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        refreshButton = findViewById(R.id.refreshButton)
        homeButton = findViewById(R.id.homeButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViewPager() {
        adapter = PlatformPagerAdapter(this, platformUrls)
        viewPager.adapter = adapter

        TabLayoutMediator(platformTabs, viewPager) { tab, position ->
            tab.text = platforms[position]
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUrlBar()
                updateNavigationButtons()
            }
        })
    }

    private fun setupListeners() {
        goButton.setOnClickListener {
            loadUrl()
        }

        urlEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                loadUrl()
                true
            } else {
                false
            }
        }

        backButton.setOnClickListener {
            getCurrentFragment()?.goBack()
            updateNavigationButtons()
        }

        forwardButton.setOnClickListener {
            getCurrentFragment()?.goForward()
            updateNavigationButtons()
        }

        refreshButton.setOnClickListener {
            getCurrentFragment()?.reload()
        }

        homeButton.setOnClickListener {
            val currentPosition = viewPager.currentItem
            getCurrentFragment()?.loadUrl(platformUrls[currentPosition])
            updateUrlBar()
        }
    }

    private fun loadUrl() {
        val url = urlEditText.text.toString().trim()
        if (url.isNotEmpty()) {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            getCurrentFragment()?.loadUrl(formattedUrl)
        }
    }

    private fun getCurrentFragment(): PlatformFragment? {
        return adapter.getFragment(viewPager.currentItem)
    }

    private fun updateUrlBar() {
        getCurrentFragment()?.getCurrentUrl()?.let { url ->
            urlEditText.setText(url)
        }
    }

    private fun updateNavigationButtons() {
        val fragment = getCurrentFragment()
        backButton.isEnabled = fragment?.canGoBack() ?: false
        forwardButton.isEnabled = fragment?.canGoForward() ?: false
    }

    fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun updateProgress(progress: Int) {
        progressBar.progress = progress
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (getCurrentFragment()?.canGoBack() == true) {
                    getCurrentFragment()?.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}

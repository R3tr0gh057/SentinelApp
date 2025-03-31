package com.app.sentinelapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import android.widget.ViewFlipper
import android.widget.ImageButton
import com.google.android.material.button.MaterialButton

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var tabLayout: TabLayout
    private lateinit var analyzeButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        viewFlipper = findViewById(R.id.viewFlipper)
        tabLayout = findViewById(R.id.tabLayout)
        analyzeButton = findViewById(R.id.analyzeButton)

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // File tab
                        viewFlipper.displayedChild = 0
                        analyzeButton.text = "Start Scan"
                    }
                    1 -> { // URL tab
                        viewFlipper.displayedChild = 1
                        analyzeButton.text = "Analyze URL"
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set up click listeners for buttons
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Set up click listener for analyze button
        analyzeButton.setOnClickListener {
            when (tabLayout.selectedTabPosition) {
                0 -> {
                    // Handle file scan
                }
                1 -> {
                    // Handle URL analysis
                }
            }
        }
    }
}
package com.app.sentinelapp

import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import android.widget.ViewFlipper
import android.widget.ImageButton
import com.google.android.material.button.MaterialButton
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.util.Log

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var tabLayout: TabLayout
    private lateinit var analyzeButton: MaterialButton
    private var lastTabPosition = 0

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
                tab?.position?.let { position ->
                    // Determine animation direction based on tab position
                    val isForward = position > lastTabPosition
                    val animation = if (isForward) {
                        AnimationUtils.loadAnimation(this@DashboardActivity, R.anim.slide_in_right)
                    } else {
                        AnimationUtils.loadAnimation(this@DashboardActivity, R.anim.slide_in_left)
                    }

                    // Set animation duration
                    animation.duration = 300

                    // Apply animation to ViewFlipper
                    viewFlipper.inAnimation = animation
                    viewFlipper.displayedChild = position

                    // Log for debugging
                    Log.d("DashboardActivity", "Tab switched to position: $position, Direction: ${if (isForward) "forward" else "backward"}")

                    // Update last position
                    lastTabPosition = position

                    // Update button text with fade animation
                    val fadeOut = AnimationUtils.loadAnimation(this@DashboardActivity, android.R.anim.fade_out)
                    val fadeIn = AnimationUtils.loadAnimation(this@DashboardActivity, android.R.anim.fade_in)
                    fadeOut.duration = 150
                    fadeIn.duration = 150

                    analyzeButton.startAnimation(fadeOut)
                    fadeOut.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            analyzeButton.text = if (position == 0) "Start Scan" else "Analyze URL"
                            analyzeButton.startAnimation(fadeIn)
                        }
                    })
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set up back button with ripple effect
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            finish()
                        }
                }
        }

        // Set up analyze button with ripple effect
        analyzeButton.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
        }
    }
}
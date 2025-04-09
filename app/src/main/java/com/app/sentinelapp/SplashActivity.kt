package com.app.sentinelapp

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.os.Handler
import android.os.Looper
import android.util.Log

class SplashActivity : AppCompatActivity() {
    private lateinit var logoAnimation: LottieAnimationView
    private lateinit var appName: android.widget.TextView
    private lateinit var loadingIndicator: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize views
        logoAnimation = findViewById(R.id.logoAnimation)
        appName = findViewById(R.id.appName)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        // Log for debugging
        Log.d("SplashActivity", "Splash screen started")

        // Start animations
        startAnimations()

        // Navigate to Login after delay
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("SplashActivity", "Navigating to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2500) // 2.5 seconds delay
    }

    private fun startAnimations() {
        // Fade in app name
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        appName.startAnimation(fadeIn)

        // Start loading indicator animation
        loadingIndicator.isIndeterminate = true

        // Log for debugging
        Log.d("SplashActivity", "Animations started")
    }
} 
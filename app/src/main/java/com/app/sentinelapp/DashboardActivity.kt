package com.app.sentinelapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.app.sentinelapp.api.VirusTotalService
import android.view.View
import android.view.animation.Animation
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import android.webkit.URLUtil
import com.google.android.material.snackbar.Snackbar

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var tabLayout: TabLayout
    private lateinit var analyzeButton: MaterialButton
    private lateinit var chooseFileButton: MaterialButton
    private lateinit var urlInput: TextInputEditText
    private var lastTabPosition = 0
    private var selectedFile: File? = null
    private val virusTotalService = VirusTotalService()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initializeViews()
        setupTabLayout()
        setupClickListeners()
    }

    private fun initializeViews() {
        viewFlipper = findViewById(R.id.viewFlipper)
        tabLayout = findViewById(R.id.tabLayout)
        analyzeButton = findViewById(R.id.analyzeButton)
        chooseFileButton = findViewById(R.id.chooseFileButton)
        urlInput = findViewById(R.id.urlInput)
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    val isForward = position > lastTabPosition
                    val animation = if (isForward) {
                        AnimationUtils.loadAnimation(this@DashboardActivity, R.anim.slide_in_right)
                    } else {
                        AnimationUtils.loadAnimation(this@DashboardActivity, R.anim.slide_in_left)
                    }

                    animation.duration = 300
                    viewFlipper.inAnimation = animation
                    viewFlipper.displayedChild = position
                    lastTabPosition = position

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
    }

    private fun setupClickListeners() {
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

        chooseFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }
            filePickerLauncher.launch(intent)
        }

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
                        .withEndAction {
                            startScan()
                        }
                }
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "scan_file")
        
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        
        selectedFile = file
        val fileName = getFileName(uri)
        chooseFileButton.text = fileName
    }

    private fun getFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
        return "Selected File"
    }

    private fun startScan() {
        val currentTab = tabLayout.selectedTabPosition
        
        if (currentTab == 0) {
            // File scan
            selectedFile?.let { file ->
                showLoading(true)
                virusTotalService.scanFile(file) { result ->
                    handleScanResult(result)
                }
            } ?: showError("Please select a file first")
        } else {
            // URL scan
            val url = urlInput.text?.toString()
            if (!url.isNullOrBlank() && URLUtil.isValidUrl(url)) {
                showLoading(true)
                virusTotalService.scanUrl(url) { result ->
                    handleScanResult(result)
                }
            } else {
                showError("Please enter a valid URL")
            }
        }
    }

    private fun handleScanResult(result: Result<String>) {
        runOnUiThread {
            showLoading(false)
            result.fold(
                onSuccess = { jsonString ->
                    val intent = Intent(this, ScanResultActivity::class.java).apply {
                        putExtra("scan_result", jsonString)
                    }
                    startActivity(intent)
                },
                onFailure = { exception ->
                    showError("Scan failed: ${exception.message}")
                }
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        runOnUiThread {
            analyzeButton.isEnabled = !isLoading
            analyzeButton.text = if (isLoading) "Scanning..." else {
                if (tabLayout.selectedTabPosition == 0) "Start Scan" else "Analyze URL"
            }
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Snackbar.make(analyzeButton, message, Snackbar.LENGTH_LONG).show()
        }
    }
}
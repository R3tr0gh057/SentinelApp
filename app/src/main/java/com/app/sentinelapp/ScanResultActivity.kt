package com.app.sentinelapp

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.sentinelapp.R
import com.app.sentinelapp.databinding.ActivityScanResultBinding
import com.app.sentinelapp.api.ApiService
import com.app.sentinelapp.api.ScanResponse
import com.app.sentinelapp.api.ScanStats
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class ScanResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanResultBinding
    private lateinit var adapter: ScanResultAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var isUrlScan = false
    private var scanId: String = ""
    private var isRequestInQueue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ScanResultActivity", "onCreate called")
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        handleIntent()
    }

    override fun onStart() {
        super.onStart()
        Log.d("ScanResultActivity", "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ScanResultActivity", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("ScanResultActivity", "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ScanResultActivity", "onStop called")
    }

    override fun onDestroy() {
        Log.d("ScanResultActivity", "onDestroy called")
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupViews() {
        try {
            adapter = ScanResultAdapter()
            binding.resultsRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@ScanResultActivity)
                adapter = this@ScanResultActivity.adapter
            }

            setupPieChart()
        } catch (e: Exception) {
            Log.e("ScanResultActivity", "Error in setupViews: ${e.message}", e)
            Toast.makeText(this, "Error setting up views", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupPieChart() {
        binding.statsChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            legend.isEnabled = true
            legend.textColor = Color.WHITE
            setDrawEntryLabels(false)
//            setEntryLabelColor(Color.WHITE)
//            setEntryLabelTextSize(12f)
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.TRANSPARENT)
            setHoleRadius(58f)
            setTransparentCircleRadius(61f)
        }
    }

    private fun handleIntent() {
        intent?.let {
            isUrlScan = it.getBooleanExtra(EXTRA_IS_URL_SCAN, false)
            scanId = it.getStringExtra(EXTRA_SCAN_ID) ?: ""
            
            if (scanId.isNotEmpty()) {
                fetchScanResults()
            } else {
                showError("Invalid scan ID")
            }
        }
    }

    private fun fetchScanResults() {
        showLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = if (isUrlScan) {
                    ApiService.getUrlScanResult(scanId)
                } else {
                    ApiService.getFileScanResult(scanId)
                }

                withContext(Dispatchers.Main) {
                    when (response.status) {
                        "completed" -> {
                            isRequestInQueue = false
                            showResults(response)
                        }
                        else -> {
                            isRequestInQueue = true
                            showQueueStatus()
                            scheduleNextFetch()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ScanResultActivity", "Error fetching results: ${e.message}", e)
                    showError("Error fetching results: ${e.message}")
                }
            }
        }
    }

    private fun showResults(response: ScanResponse) {
        binding.loadingProgress.visibility = View.GONE
        binding.resultSummaryText.visibility = View.VISIBLE
        binding.resultsRecyclerView.visibility = View.VISIBLE
        binding.statsChart.visibility = View.VISIBLE
        binding.resultsTitle.visibility = View.VISIBLE
        binding.infoCard.visibility = View.VISIBLE
        binding.chartTitle.visibility = View.VISIBLE
        binding.fileInfo.visibility = View.VISIBLE

        // Update summary
        val threatCount = response.results.count { it.isThreat }
        binding.resultSummaryText.text = if (threatCount > 0) {
            getString(R.string.threats_found_summary, threatCount)
        } else {
            getString(R.string.no_threats_found)
        }

        // Update pie chart
        updatePieChart(response.stats)

        // Update file/URL info
        updateFileInfo(response)

        // Update detailed results
        adapter.updateResults(response.results)

        // Show appropriate animation
        binding.animationView.setAnimation(
            if (threatCount > 0) R.raw.danger_animation
            else R.raw.success_animation
        )
    }

    private fun updatePieChart(stats: ScanStats) {
        try {
            val entries = mutableListOf<PieEntry>()
            val colors = mutableListOf<Int>()

            // Add entries for each stat that is greater than 0
            if (stats.malicious > 0) {
                entries.add(PieEntry(stats.malicious.toFloat(), "Malicious"))
                colors.add(Color.RED)
            }
            if (stats.suspicious > 0) {
                entries.add(PieEntry(stats.suspicious.toFloat(), "Suspicious"))
                colors.add(Color.rgb(255, 165, 0)) // Orange
            }
            if (stats.undetected > 0) {
                entries.add(PieEntry(stats.undetected.toFloat(), "Undetected"))
                colors.add(Color.GRAY)
            }
            if (stats.harmless > 0) {
                entries.add(PieEntry(stats.harmless.toFloat(), "Harmless"))
                colors.add(Color.GREEN)
            }
            if (stats.failure > 0) {
                entries.add(PieEntry(stats.failure.toFloat(), "Failure"))
                colors.add(Color.rgb(128, 0, 128)) // Purple
            }
            if (stats.typeUnsupported > 0) {
                entries.add(PieEntry(stats.typeUnsupported.toFloat(), "Unsupported"))
                colors.add(Color.rgb(139, 69, 19)) // Brown
            }
            if (stats.timeout > 0) {
                entries.add(PieEntry(stats.timeout.toFloat(), "Timeout"))
                colors.add(Color.LTGRAY)
            }
            // Handle empty data case
            if (entries.isEmpty()) {
                binding.statsChart.visibility = View.GONE
                return
            }

            val dataSet = PieDataSet(entries, "Scan Results").apply {
                this.colors = colors
                valueTextSize = 12f
                valueTextColor = Color.WHITE
                valueFormatter = PercentFormatter(binding.statsChart)
            }

            binding.statsChart.data = PieData(dataSet)
            binding.statsChart.invalidate()
        } catch (e: Exception) {
            Log.e("ScanResultActivity", "Error updating pie chart: ${e.message}", e)
            binding.statsChart.visibility = View.GONE
        }
    }

    private fun updateFileInfo(response: ScanResponse) {
        // Update file hashes and size
        response.fileInfo?.let { info ->
            binding.sha256Text.text = buildString {
        append("SHA256: ")
        append(info.sha256)
    }
            binding.md5Text.text = buildString {
        append("MD5: ")
        append(info.md5)
    }
            binding.sha1Text.text = buildString {
        append("SHA1: ")
        append(info.sha1)
    }
            binding.sizeText.text = buildString {
        append("Size: ")
        append(formatFileSize(info.size))
    }
        }

        // Update URL info if it's a URL scan
        if (isUrlScan) {
            response.urlInfo?.let { info ->
                binding.urlText.text = buildString {
        append("URL: ")
        append(info.url)
    }
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = size.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return DecimalFormat("#.##").format(value) + " " + units[unitIndex]
    }

    private fun showQueueStatus() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.resultsTitle.visibility = View.GONE
        binding.chartTitle.visibility = View.GONE
        binding.fileInfo.visibility = View.GONE
        binding.resultSummaryText.text = getString(R.string.request_in_queue)
        binding.resultsRecyclerView.visibility = View.GONE
        binding.statsChart.visibility = View.GONE
        binding.infoCard.visibility = View.GONE
    }

    private fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.chartTitle.visibility = View.GONE
        binding.fileInfo.visibility = View.GONE
        binding.resultsTitle.visibility = View.GONE
        binding.resultsRecyclerView.visibility = View.GONE
        binding.statsChart.visibility = View.GONE
        binding.infoCard.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingProgress.visibility = View.GONE
        binding.resultSummaryText.visibility = View.VISIBLE
        binding.resultSummaryText.text = message
        binding.resultsRecyclerView.visibility = View.GONE
        binding.statsChart.visibility = View.GONE
        binding.infoCard.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun scheduleNextFetch() {
        if (isRequestInQueue) {
            handler.postDelayed({
                fetchScanResults()
            }, REFRESH_DELAY)
        }
    }

    companion object {
        const val EXTRA_SCAN_ID = "extra_scan_id"
        const val EXTRA_IS_URL_SCAN = "extra_is_url_scan"
        private const val REFRESH_DELAY = 5000L // 5 seconds
    }
}
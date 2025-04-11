package com.app.sentinelapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.sentinelapp.R
import com.app.sentinelapp.databinding.ActivityScanResultBinding
import com.app.sentinelapp.api.ApiService
import com.app.sentinelapp.api.ScanResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanResultBinding
    private lateinit var adapter: ScanResultAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var isUrlScan = false
    private var scanId: String = ""
    private var isRequestInQueue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        handleIntent()
    }

    private fun setupViews() {
        adapter = ScanResultAdapter()
        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ScanResultActivity)
            adapter = this@ScanResultActivity.adapter
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
                    showError("Error fetching results: ${e.message}")
                }
            }
        }
    }

    private fun showResults(response: ScanResponse) {
        binding.loadingProgress.visibility = View.GONE
        binding.resultSummaryText.visibility = View.GONE
        binding.resultsRecyclerView.visibility = View.VISIBLE

        // Update summary
        val threatCount = response.results.count { it.isThreat }
        binding.resultSummaryText.text = if (threatCount > 0) {
            getString(R.string.threats_found_summary, threatCount)
        } else {
            getString(R.string.no_threats_found)
        }

        // Update detailed results
        adapter.updateResults(response.results)
    }

    private fun showQueueStatus() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.resultSummaryText.visibility = View.VISIBLE
        binding.resultSummaryText.text = getString(R.string.request_in_queue)
        binding.resultsRecyclerView.visibility = View.GONE
    }

    private fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.resultSummaryText.visibility = View.GONE
        binding.resultsRecyclerView.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingProgress.visibility = View.GONE
        binding.resultSummaryText.visibility = View.VISIBLE
        binding.resultSummaryText.text = message
        binding.resultsRecyclerView.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun scheduleNextFetch() {
        if (isRequestInQueue) {
            handler.postDelayed({
                fetchScanResults()
            }, REFRESH_DELAY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val EXTRA_SCAN_ID = "extra_scan_id"
        const val EXTRA_IS_URL_SCAN = "extra_is_url_scan"
        private const val REFRESH_DELAY = 5000L // 5 seconds
    }
}
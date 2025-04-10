package com.app.sentinelapp

import android.graphics.Color
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.widget.ImageButton
import android.widget.TextView
import com.github.mikephil.charting.charts.PieChart
import org.json.JSONObject

class ScanResultActivity : AppCompatActivity() {
    private lateinit var sha256Text: TextView
    private lateinit var md5Text: TextView
    private lateinit var sha1Text: TextView
    private lateinit var sizeText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var scanResultAdapter: ScanResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)

        initializeViews()
        setupClickListeners()
        setupRecyclerView()
        setupPieChart()

        // Get scan result from intent
        intent.getStringExtra("scan_result")?.let { jsonResult ->
            displayResults(jsonResult)
        }
    }

    private fun initializeViews() {
        sha256Text = findViewById(R.id.sha256Text)
        md5Text = findViewById(R.id.md5Text)
        sha1Text = findViewById(R.id.sha1Text)
        sizeText = findViewById(R.id.sizeText)
        pieChart = findViewById(R.id.pieChart)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        scanResultAdapter = ScanResultAdapter()
        resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ScanResultActivity)
            adapter = scanResultAdapter
        }
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 70f
            setDrawCenterText(true)
            setCenterTextColor(getColor(R.color.white))
            setCenterTextSize(20f)
            legend.isEnabled = false
            setTouchEnabled(false)
        }
    }

    private fun displayResults(jsonResult: String) {
        try {
            val jsonResponse = JSONObject(jsonResult)
            val data = jsonResponse.getJSONObject("data")
            val attributes = data.getJSONObject("attributes")
            val meta = jsonResponse.getJSONObject("meta")
            val fileInfo = meta.getJSONObject("file_info")

            // Display file information
            sha256Text.text = fileInfo.getString("sha256")
            md5Text.text = fileInfo.getString("md5")
            sha1Text.text = fileInfo.getString("sha1")
            sizeText.text = Formatter.formatFileSize(this, fileInfo.getLong("size"))

            // Get stats
            val stats = attributes.getJSONObject("stats")
            updatePieChart(stats)

            // Parse and display results
            val results = attributes.getJSONObject("results")
            val resultItems = mutableListOf<ScanResultItem>()

            results.keys().forEach { key ->
                val result = results.getJSONObject(key)
                resultItems.add(
                    ScanResultItem(
                        engineName = result.getString("engine_name"),
                        category = result.getString("category"),
                        result = result.optString("result", "N/A")
                    )
                )
            }

            scanResultAdapter.submitList(resultItems)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePieChart(stats: JSONObject) {
        val undetected = stats.getInt("undetected")
        val unsupported = stats.getInt("type-unsupported")
        val total = undetected + unsupported

        val entries = listOf(
            PieEntry(undetected.toFloat(), ""),
            PieEntry(unsupported.toFloat(), "")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                getColor(R.color.cyan),
                getColor(R.color.text_secondary)
            )
            setDrawValues(false)
        }

        pieChart.apply {
            data = PieData(dataSet)
            centerText = undetected.toString()
            invalidate()
        }
    }
}
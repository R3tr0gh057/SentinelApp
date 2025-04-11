package com.app.sentinelapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.sentinelapp.api.ScanResult
import com.app.sentinelapp.databinding.ItemScanResultBinding

class ScanResultAdapter : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {
    private var results: List<ScanResult> = emptyList()

    fun updateResults(newResults: List<ScanResult>) {
        results = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size

    class ViewHolder(
        private val binding: ItemScanResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: ScanResult) {
            binding.apply {
                engineNameText.text = result.engineName
                categoryText.text = result.category
                resultText.text = result.result
                
                // Set threat indicator color based on isThreat
                threatIndicator.setImageResource(
                    if (result.isThreat) R.drawable.ic_threat
                    else R.drawable.ic_clean
                )
                
                threatIndicator.setColorFilter(
                    itemView.context.getColor(
                        if (result.isThreat) R.color.danger
                        else R.color.success
                    )
                )
            }
        }
    }
} 
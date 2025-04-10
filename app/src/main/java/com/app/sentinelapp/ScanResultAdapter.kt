package com.app.sentinelapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ScanResultAdapter : ListAdapter<ScanResultItem, ScanResultAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val engineNameText: TextView = view.findViewById(R.id.engineNameText)
        private val categoryText: TextView = view.findViewById(R.id.categoryText)
        private val resultText: TextView = view.findViewById(R.id.resultText)

        fun bind(item: ScanResultItem) {
            engineNameText.text = item.engineName
            categoryText.text = item.category
            resultText.text = item.result
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ScanResultItem>() {
        override fun areItemsTheSame(oldItem: ScanResultItem, newItem: ScanResultItem): Boolean {
            return oldItem.engineName == newItem.engineName
        }

        override fun areContentsTheSame(oldItem: ScanResultItem, newItem: ScanResultItem): Boolean {
            return oldItem == newItem
        }
    }
} 
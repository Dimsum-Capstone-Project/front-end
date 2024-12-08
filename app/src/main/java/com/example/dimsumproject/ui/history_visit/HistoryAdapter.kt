package com.example.dimsumproject.ui.history_visit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dimsumproject.data.api.HistoryItem
import com.example.dimsumproject.databinding.ItemHistoryBinding
import com.example.dimsumproject.databinding.ItemHistoryHeaderBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>() // Can be String (header) or HistoryItem
    private val filteredItems = mutableListOf<Any>()
    private var currentQuery: String = ""

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is String -> TYPE_HEADER
            is HistoryItem -> TYPE_ITEM
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    private fun getItem(position: Int): Any {
        return if (currentQuery.isEmpty()) items[position] else filteredItems[position]
    }

    class HeaderViewHolder(private val binding: ItemHistoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: String) {
            binding.tvHeader.text = header
        }
    }

    class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem) {
            binding.apply {
                tvName.text = item.profile.name
                tvDate.text = formatDate(item.time_scanned)
            }
        }

        private fun formatDate(timeScanned: String): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, MMM dd yyyy", Locale.getDefault())
            val date = inputFormat.parse(timeScanned)
            return outputFormat.format(date!!)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemHistoryHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            TYPE_ITEM -> {
                val binding = ItemHistoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HistoryViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is String -> (holder as HeaderViewHolder).bind(item)
            is HistoryItem -> {
                (holder as HistoryViewHolder).bind(item)
                holder.itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun getItemCount(): Int =
        if (currentQuery.isEmpty()) items.size else filteredItems.size

    fun submitList(historyItems: List<HistoryItem>) {
        items.clear()
        // Group items by date
        val groupedItems = historyItems.groupBy { item ->
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(item.time_scanned)
            val today = Calendar.getInstance()
            val itemDate = Calendar.getInstance().apply { time = date!! }

            when {
                isSameDay(itemDate, today) -> "Terbaru (hari ini)"
                isYesterday(itemDate, today) -> "Kemarin"
                else -> "Lebih lama"
            }
        }

        // Add items with headers
        groupedItems.forEach { (header, groupItems) ->
            items.add(header)
            items.addAll(groupItems)
        }

        filter(currentQuery)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(date: Calendar, today: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(date, yesterday)
    }

    fun filter(query: String) {
        currentQuery = query
        filteredItems.clear()
        if (query.isEmpty()) {
            filteredItems.addAll(items)
        } else {
            val filtered = items.filter {
                when (it) {
                    is HistoryItem -> it.profile.name.contains(query, ignoreCase = true)
                    is String -> true // Keep headers
                    else -> false
                }
            }
            filteredItems.addAll(filtered)
        }
        notifyDataSetChanged()
    }
}
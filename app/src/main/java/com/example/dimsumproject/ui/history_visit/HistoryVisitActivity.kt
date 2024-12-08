package com.example.dimsumproject.ui.history_visit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.R
import com.example.dimsumproject.data.api.HistoryItem
import com.example.dimsumproject.databinding.ActivityHistoryVisitBinding
import com.example.dimsumproject.ui.scan.ScanActivity
import com.example.dimsumproject.ui.settings.SettingsActivity
import com.google.android.material.tabs.TabLayout

class HistoryVisitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryVisitBinding
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter
    private var isWhoScannedMe = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryVisitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        loadData()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.navigation_history
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish()
                    overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
                    true
                }
                R.id.navigation_history -> {
                    // Sudah di history, tidak perlu melakukan apa-apa
                    true
                }
                R.id.navigation_scan -> {
                    startActivity(Intent(this, ScanActivity::class.java))
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.navigation_logout -> {
                    // Handle logout
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().clear().apply()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set history sebagai item yang aktif
        binding.bottomNav.selectedItemId = R.id.navigation_history
    }

    private fun setupViews() {
        // Setup TabLayout
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Who Scanned Me"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Who I Scanned"))

        // Setup RecyclerView
        historyAdapter = HistoryAdapter { showHistoryDetail(it) }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryVisitActivity)
            adapter = historyAdapter
        }

        // Setup SwipeRefresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHistory()
        }

        // Setup SearchView
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                historyAdapter.filter(newText ?: "")
                return true
            }
        })

        // Setup TabLayout listener
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isWhoScannedMe = tab?.position == 0
                updateHistoryList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingCard.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.historyData.observe(this) {
            updateHistoryList()
        }

        viewModel.error.observe(this) { error ->
            // Handle error
        }

        viewModel.navigateToLogin.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun updateHistoryList() {
        val data = viewModel.historyData.value
        val items = if (isWhoScannedMe) data?.who_scanned_me else data?.who_i_scanned
        items?.let {
            if (it.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvHistory.visibility = View.VISIBLE
                historyAdapter.submitList(it)
            }
        }
    }

    private fun showHistoryDetail(item: HistoryItem) {
        HistoryDetailDialog(this, item).show()
    }

    private fun loadData() {
        viewModel.loadHistory()
    }

}
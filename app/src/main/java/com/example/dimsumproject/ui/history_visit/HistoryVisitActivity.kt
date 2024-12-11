package com.example.dimsumproject.ui.history_visit

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.R
import com.example.dimsumproject.Utils
import com.example.dimsumproject.data.api.HistoryItem
import com.example.dimsumproject.databinding.ActivityHistoryVisitBinding
import com.example.dimsumproject.ui.scan.ScanActivity
import com.example.dimsumproject.ui.settings.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

class HistoryVisitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryVisitBinding
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter
    private var isWhoScannedMe = true
    private lateinit var utils: Utils
    private var currentImageUri: Uri? = null
    private var isScanningMode = false

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            navigateToScanWithUri(currentImageUri)
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navigateToScanWithUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryVisitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils = Utils(applicationContext)
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
                    showImageSourceDialog()
                    false
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.navigation_logout -> {
                    showLogoutConfirmation()
                    true
                }
                else -> false
            }
        }

        // Set history sebagai item yang aktif
        binding.bottomNav.selectedItemId = R.id.navigation_history

        // Setup FAB Scan
        binding.fabScan.setOnClickListener {
            showImageSourceDialog()
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Palm Image Source")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndStart()
                    1 -> launcherIntentGallery.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        currentImageUri = utils.getImageUri()
        launcherIntentCamera.launch(currentImageUri!!)
    }

    private fun navigateToScanWithUri(uri: Uri?) {
        uri?.let {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("image_uri", it.toString())
            startActivity(intent)
        }
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
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }

        viewModel.navigateToLogin.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
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
        HistoryBottomSheet(this, item).show()
    }

    private fun loadData() {
        viewModel.loadHistory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            binding.rvHistory.adapter = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
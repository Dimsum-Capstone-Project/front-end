package com.example.dimsumproject.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.R
import com.example.dimsumproject.databinding.ActivityHomeBinding
import com.example.dimsumproject.ui.history_visit.HistoryVisitActivity
import com.example.dimsumproject.ui.scan.ScanActivity
import com.example.dimsumproject.ui.settings.SettingsActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add back pressed callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        // Show loading immediately
        showLoading()

        // Check token before loading data
        if (!checkAccessToken()) {
            clearAccessToken()
            redirectToLogin()
            return
        }

        setupRecyclerView()
        setupObservers()
        loadData()
        setupNavigation()


    }

    private fun setupRecyclerView() {
        binding.rvContacts.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = ContactsAdapter(emptyList()) // Set adapter kosong dulu
            Log.d("HomeActivity", "RecyclerView setup completed")
        }
    }

    private fun setupObservers() {
        // Loading Observer
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        // Profile Observer
        viewModel.profile.observe(this) { profile ->
            // Load background image (tetap sama)
            Glide.with(this)
                .load("https://cdn.idntimes.com/content-images/community/2024/06/img-20240605-192130-2b64a83f842f8dac9ad37a9c9fa77858_600x400.jpg")
                .centerCrop()
                .into(binding.ivBackground)

            // Load profile picture dari URL baru
            val profileImageUrl = "https://storage.googleapis.com/dimsum_palm_public/${profile.profile_picture}"

            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(binding.ivProfile)

            // Set profile information
            binding.tvUsername.text = profile.username
            binding.tvBio.text = profile.bio ?: "No bio added"
            binding.tvJobTitle.text = profile.job_title ?: "No job title"
        }

        // Contacts Observer
        viewModel.contacts.observe(this) { contactResponse ->
            binding.rvContacts.adapter = ContactsAdapter(contactResponse.contacts)
        }

        // Error Observer
        viewModel.error.observe(this) { error ->
            when {
                error.contains("401") ||
                        error.contains("403") ||
                        error.contains("Could not validate credentials") -> {
                    clearAccessToken()
                    redirectToLogin()
                }
                else -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

        // Navigation Observer
        viewModel.navigateToLogin.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                clearAccessToken()
                redirectToLogin()
            }
        }

        viewModel.contacts.observe(this) { contactResponse ->
            Log.d("HomeActivity", "Received ${contactResponse.contacts.size} contacts")
            binding.rvContacts.adapter = ContactsAdapter(contactResponse.contacts)
        }

        viewModel.error.observe(this) { error ->
            Log.e("HomeActivity", "Error: $error")
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true  // Tidak perlu melakukan apa-apa
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryVisitActivity::class.java))
                    false  // Kembalikan false agar item tidak terselect
                }
                R.id.navigation_scan -> {
                    startActivity(Intent(this, ScanActivity::class.java))
                    false
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                R.id.navigation_logout -> {
                    clearAccessToken()
                    redirectToLogin()
                    false
                }
                else -> false
            }
        }
    }

    private fun showLoading() {
        binding.loadingCard.visibility = View.VISIBLE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideLoading() {
        binding.loadingCard.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun loadData() {
        Log.d("HomeActivity", "Starting to load data...")
        viewModel.loadProfile() // loadContacts akan dipanggil otomatis setelah profile berhasil
    }


    private fun checkAccessToken(): Boolean {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("access_token", null)
        return !token.isNullOrEmpty()
    }

    private fun clearAccessToken() {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("access_token").apply()
    }

    private fun redirectToLogin() {
        // Clear token
        clearAccessToken()

        // Redirect ke MainActivity dengan flag baru
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
package com.example.dimsumproject.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.R
import com.example.dimsumproject.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        loadData()
    }

    private fun setupRecyclerView() {
        binding.rvContacts.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
        }
    }

    private fun setupObservers() {
        // Profile Observer
        viewModel.profile.observe(this) { profile ->
            // Load background image
            Glide.with(this)
                .load("https://cdn.idntimes.com/content-images/community/2024/06/img-20240605-192130-2b64a83f842f8dac9ad37a9c9fa77858_600x400.jpg")
                .centerCrop()
                .into(binding.ivBackground)

            // Load profile picture with white border
            Glide.with(this)
                .load(profile.profile_picture ?: "")
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
            // Set adapter with contacts data
            binding.rvContacts.adapter = ContactsAdapter(contactResponse.contacts)
        }

        // Error Observer
        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }

        // Navigation Observer
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

    private fun loadData() {
        viewModel.loadProfile()
        viewModel.loadContacts()
    }
}
package com.example.dimsumproject.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.R
import com.example.dimsumproject.Utils
import com.example.dimsumproject.databinding.ActivityHomeBinding
import com.example.dimsumproject.ui.scan.ScanActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var utils: Utils
    private var currentImageUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private lateinit var contactsAdapter: ContactsAdapter

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
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils = Utils(applicationContext)
        setupRecyclerView()
        setupObservers()
        setupFabScan()
        loadData()
    }

    private fun setupFabScan() {
        binding.fabScan.setOnClickListener {
            showImageSourceDialog()
        }
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

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(emptyList())
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
}
package com.example.dimsumproject.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper

import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.dimsumproject.data.api.Contact
import com.example.dimsumproject.databinding.ActivityHomeBinding
import com.example.dimsumproject.ui.history_visit.HistoryVisitActivity
import com.example.dimsumproject.ui.scan.ScanActivity
import com.example.dimsumproject.ui.settings.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    private val contactViewModel: ContactViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var utils: Utils
    private var currentImageUri: Uri? = null

    // Scanner related properties
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

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils = Utils(applicationContext)

        setupBackPressCallback()

        // Show loading and check token
        showLoading()
        if (!checkAccessToken()) {
            clearAccessToken()
            redirectToLogin()
            return
        }

        setupRecyclerView()
        setupObservers()
        setupFabScan()
        setupContactActions()
        loadData()
        setupNavigation()
    }

    private fun setupBackPressCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(
            contacts = emptyList(),
            onEditClick = { contact -> showEditContactSheet(contact) },
            onDeleteClick = { contact -> showDeleteConfirmation(contact) }
        )
        binding.rvContacts.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = contactsAdapter
        }
    }

    private fun setupObservers() {
        // Loading Observer
        profileViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        contactViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        // Profile Observer
        profileViewModel.profile.observe(this) { profile ->
            // Load background image
            Glide.with(this)
                .load("https://cdn.idntimes.com/content-images/community/2024/06/img-20240605-192130-2b64a83f842f8dac9ad37a9c9fa77858_600x400.jpg")
                .centerCrop()
                .into(binding.ivBackground)

            // Load profile picture
            val profileImageUrl =
                "https://storage.googleapis.com/dimsum_palm_public/${profile.profile_picture}"
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
        profileViewModel.contacts.observe(this) { contactResponse ->
            contactsAdapter.updateContacts(contactResponse.contacts)
        }

        // Contact Operations Observers
        contactViewModel.success.observe(this) { message ->
            if (message.isNotEmpty()) {
                showSuccessSnackbar(message)
                contactViewModel.resetMessages()
                loadData() // Refresh data after successful operation
            }
        }

        contactViewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                showErrorSnackbar(error)
                contactViewModel.resetMessages()
            }
        }

        // Error & Navigation Observer
        profileViewModel.error.observe(this) { error ->
            when {
                error.contains("401") ||
                        error.contains("403") ||
                        error.contains("Could not validate credentials") -> {
                    clearAccessToken()
                    redirectToLogin()
                }

                else -> showErrorSnackbar(error)
            }
        }

        profileViewModel.navigateToLogin.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                clearAccessToken()
                redirectToLogin()
            }
        }
    }

    private fun setupContactActions() {
        binding.fabAddContact.setOnClickListener {
            showAddContactSheet()
        }
    }

    private fun showAddContactSheet() {
        ContactBottomSheet(
            context = this,
            mode = ContactBottomSheet.Mode.ADD,
            onSubmit = { type, value, notes ->
                contactViewModel.addContact(type, value, notes, getStoredToken())
            }
        ).show()
    }

    private fun showEditContactSheet(contact: Contact) {
        ContactBottomSheet(
            context = this,
            mode = ContactBottomSheet.Mode.EDIT,
            contact = contact,
            onSubmit = { _, value, notes ->
                contactViewModel.editContact(
                    id = contact.contact_id!!,
                    type = contact.contact_type,
                    value = value,
                    notes = notes,
                    token = getStoredToken()
                )
            }
        ).show()
    }

    private fun showDeleteConfirmation(contact: Contact) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Yes, delete it") { _, _ ->
                contactViewModel.deleteContact(contact.contact_id!!, getStoredToken())
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showSuccessSnackbar(message: String) {
        val snackbar = Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).apply {
            setBackgroundTint(
                ContextCompat.getColor(
                    this@HomeActivity,
                    android.R.color.holo_green_light
                )
            )
            setTextColor(ContextCompat.getColor(this@HomeActivity, android.R.color.white))
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (snackbar.isShown) snackbar.dismiss()
        }, 5000)

        snackbar.show()
    }

    private fun showErrorSnackbar(message: String) {
        val snackbar = Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).apply {
            setBackgroundTint(
                ContextCompat.getColor(
                    this@HomeActivity,
                    android.R.color.holo_red_light
                )
            )
            setTextColor(ContextCompat.getColor(this@HomeActivity, android.R.color.white))
            setActionTextColor(ContextCompat.getColor(this@HomeActivity, android.R.color.white))
        }

        snackbar.setAction("RETRY") {
            loadData()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (snackbar.isShown) snackbar.dismiss()
        }, 5000)

        snackbar.show()
    }

    private fun loadData() {
        profileViewModel.loadProfile()
        profileViewModel.loadContacts()
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

    // Scanner related functions
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

    // Permission result
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

    // Token and Navigation related functions
    private fun getStoredToken(): String {
        return getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("access_token", "") ?: ""
    }

    private fun checkAccessToken(): Boolean {
        val token = getStoredToken()
        return token.isNotEmpty()
    }

    private fun clearAccessToken() {
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .edit()
            .remove("access_token")
            .apply()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryVisitActivity::class.java))
                    false
                }

                R.id.navigation_scan -> {
                    showImageSourceDialog()
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
}
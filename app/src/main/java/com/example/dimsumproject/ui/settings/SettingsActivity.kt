package com.example.dimsumproject.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.R
import com.example.dimsumproject.Utils
import com.example.dimsumproject.data.api.ProfileResponse
import com.example.dimsumproject.databinding.ActivitySettingsBinding
import com.example.dimsumproject.ui.history_visit.HistoryVisitActivity
import com.example.dimsumproject.ui.scan.ScanActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var utils: Utils
    private var currentImageUri: Uri? = null
    private var isScanningMode = false

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            if (currentImageUri != null) {
                navigateToScanWithUri(currentImageUri)
            } else {
                handleNewProfileImage(currentImageUri)
            }
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (isScanningMode) {
                navigateToScanWithUri(it)
            } else {
                handleNewProfileImage(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils = Utils(applicationContext)
        setupUI()
        setupObservers()
        setupNavigation()
        setupSwipeRefresh()
        loadData()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
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
                    startActivity(Intent(this, HistoryVisitActivity::class.java))
                    finish()
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.navigation_scan -> {
                    isScanningMode = true
                    showImageSourceDialog()
                    false
                }
                R.id.navigation_settings -> {
                    true
                }
                R.id.navigation_logout -> {
                    showLogoutConfirmation()
                    true
                }
                else -> false
            }
        }

        binding.bottomNav.selectedItemId = R.id.navigation_settings

        binding.fabScan.setOnClickListener {
            isScanningMode = true
            showImageSourceDialog()
        }
    }

    private fun setupUI() {
        binding.apply {
            editProfileImageButton.setOnClickListener {
                isScanningMode = false
                showImagePickerDialog()
            }

            informationMenu.setOnClickListener {
                viewModel.profile.value?.let { profile ->
                    showEditProfileBottomSheet(profile)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.apply {
            profile.observe(this@SettingsActivity) { profile ->
                updateUI(profile)
                binding.swipeRefresh.isRefreshing = false
            }

            isLoading.observe(this@SettingsActivity) { isLoading ->
                binding.loadingCard.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (!isLoading) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }

            loadingTimeout.observe(this@SettingsActivity) { timeout ->
                binding.loadingText.visibility = if (timeout) View.VISIBLE else View.GONE
            }

            error.observe(this@SettingsActivity) { error ->
                showError(error)
                binding.swipeRefresh.isRefreshing = false
            }

            updateSuccess.observe(this@SettingsActivity) { success ->
                if (success) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadData()
                }
            }

            navigateToLogin.observe(this@SettingsActivity) { shouldNavigate ->
                if (shouldNavigate) {
                    navigateToLogin()
                }
            }
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

    private fun showImagePickerDialog() {
        isScanningMode = false
        AlertDialog.Builder(this)
            .setTitle("Choose Profile Picture")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> launcherIntentGallery.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
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
        isScanningMode = false
    }

    private fun handleNewProfileImage(uri: Uri?) {
        uri?.let {
            val file = getFileFromUri(it)
            file?.let { imageFile ->
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData(
                    "profile_picture",
                    imageFile.name,
                    requestFile
                )

                binding.loadingCard.visibility = View.VISIBLE
                viewModel.profile.value?.let { currentProfile ->
                    viewModel.updateProfile(
                        currentProfile.username ?: "",
                        currentProfile.bio ?: "",
                        currentProfile.company ?: "",
                        currentProfile.job_title ?: "",
                        imagePart
                    )
                }
            }
        }
        isScanningMode = false
    }

    private fun showEditProfileBottomSheet(profile: ProfileResponse) {
        EditProfileBottomSheet(
            this,
            profile
        ) { username, bio, company, jobTitle ->
            binding.loadingCard.visibility = View.VISIBLE
            viewModel.updateProfile(
                username,
                bio,
                company,
                jobTitle,
                null
            )
        }.show()
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
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadData() {
        if (!binding.swipeRefresh.isRefreshing) {
            binding.loadingCard.visibility = View.VISIBLE
        }
        viewModel.loadProfile()
    }

    private fun updateUI(profile: ProfileResponse) {
        Log.d("SettingsActivity", "Updating UI with profile: $profile")
        binding.apply {
            usernameText.text = profile.username
            Log.d("SettingsActivity", "Username set to: ${profile.username}")
            emailText.text = profile.email
            Log.d("SettingsActivity", "Email set to: ${profile.email}")

            // Load profile picture dari URL baru
            val profileImageUrl = "https://storage.googleapis.com/dimsum_palm_public/${profile.profile_picture}"
            Log.d("SettingsActivity", "Loading image from: $profileImageUrl")

            Glide.with(this@SettingsActivity)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loadingCard.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loadingCard.visibility = View.GONE
                        return false
                    }
                })
                .into(profileImage)
        }
    }

    private fun showError(error: ProfileUpdateError) {
        binding.loadingCard.visibility = View.GONE
        Toast.makeText(this, error.getErrorMessage(), Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("profile_", ".jpg", cacheDir)
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
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

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.navigation_settings
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            binding.profileImage.setImageDrawable(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}
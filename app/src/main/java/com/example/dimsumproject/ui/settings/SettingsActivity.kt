package com.example.dimsumproject.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.R
import com.example.dimsumproject.Utils
import com.example.dimsumproject.data.api.ProfileResponse
import com.example.dimsumproject.databinding.ActivitySettingsBinding
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

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            handleNewProfileImage(currentImageUri)
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleNewProfileImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils = Utils(applicationContext)
        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        binding.apply {
            // Profile Image Edit
            editProfileImageButton.setOnClickListener {
                showImagePickerDialog()
            }

            // Information Menu
            informationMenu.setOnClickListener {
                viewModel.profile.value?.let { profile ->
                    showEditProfileBottomSheet(profile)
                }
            }

            // Logout
            logoutButton.setOnClickListener {
                showLogoutConfirmation()
            }
        }
    }

    private fun setupObservers() {
        viewModel.apply {
            profile.observe(this@SettingsActivity) { profile ->
                updateUI(profile)
            }

            isLoading.observe(this@SettingsActivity) { isLoading ->
                binding.loadingCard.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            loadingTimeout.observe(this@SettingsActivity) { timeout ->
                binding.loadingText.visibility = if (timeout) View.VISIBLE else View.GONE
            }

            error.observe(this@SettingsActivity) { error ->
                showError(error)
            }

            navigateToLogin.observe(this@SettingsActivity) { shouldNavigate ->
                if (shouldNavigate) {
                    navigateToLogin()
                }
            }
        }
    }

    private fun updateUI(profile: ProfileResponse) {
        binding.apply {
            usernameText.text = profile.username
            emailText.text = profile.email

            Glide.with(this@SettingsActivity)
                .load(profile.profile_picture)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(profileImage)
        }
    }

    private fun showImagePickerDialog() {
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

    private fun startCamera() {
        currentImageUri = utils.getImageUri()
        launcherIntentCamera.launch(currentImageUri!!)
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

                // Update profile with new image
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
    }

    private fun showEditProfileBottomSheet(profile: ProfileResponse) {
        EditProfileBottomSheet(
            this,
            profile
        ) { username, bio, company, jobTitle ->
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
        // Clear preferences
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showError(error: ProfileUpdateError) {
        Toast.makeText(this, error.getErrorMessage(), Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadData() {
        viewModel.loadProfile()
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

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}
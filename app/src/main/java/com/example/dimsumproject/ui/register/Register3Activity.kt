package com.example.dimsumproject.ui.register

import android.R
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dimsumproject.Utils
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.ApiService
import com.example.dimsumproject.data.api.ContactResponse
import com.example.dimsumproject.data.api.ProfileResponse
import com.example.dimsumproject.databinding.ActivityRegister3Binding
import com.example.dimsumproject.databinding.DialogRegistrationSuccessBinding
import com.example.dimsumproject.ui.login.LoginActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class Register3Activity : AppCompatActivity() {
        private lateinit var binding: ActivityRegister3Binding
        private var currentProfileImageUri: Uri? = null
        private lateinit var utils: Utils
        private lateinit var token: String
        private lateinit var username: String
        private lateinit var contactAdapter: RegisterContactAdapter

        private val launcherGallery = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                currentProfileImageUri = it
                binding.profileImageView.setImageURI(it)
            }
        }

        private val launcherCamera = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { isSuccess ->
            if (isSuccess) {
                showProfileImage()
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityRegister3Binding.inflate(layoutInflater)
            setContentView(binding.root)

            token = intent.getStringExtra("token") ?: run {
                showToast("Token not found")
                finish()
                return
            }

            username = intent.getStringExtra("username") ?: run {  // Tambahkan ini
                showToast("Username not found")
                finish()
                return
            }

            utils = Utils(applicationContext)
            setupClickListeners()
            setupContactInput()
        }

        private fun setupContactInput() {
            // Setup adapter
            contactAdapter = RegisterContactAdapter(mutableListOf()) { position ->
                contactAdapter.removeContact(position)
            }

            // Setup RecyclerView
            binding.rvContacts.apply {
                layoutManager = LinearLayoutManager(this@Register3Activity)
                adapter = contactAdapter
            }

            // Setup Spinner/Dropdown untuk contact type
            val contactTypes = arrayOf("IG", "WA", "FB", "X", "LI")
            val spinnerAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, contactTypes)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerContactType.adapter = spinnerAdapter

            // Handle add contact button
            binding.btnAddContact.setOnClickListener {
                val contactType = binding.spinnerContactType.selectedItem.toString()
                val contactValue = binding.etContactValue.text.toString()
                val notes = binding.etNotes.text.toString()

                if (contactValue.isNotEmpty()) {
                    val contact = RegisterContact(contactType, contactValue, notes)
                    contactAdapter.addContact(contact)

                    // Clear inputs
                    binding.etContactValue.text?.clear()
                    binding.etNotes.text?.clear()
                } else {
                    showToast("Please enter contact value")
                }
            }
        }

        private fun setupClickListeners() {
            binding.changeProfileButton.setOnClickListener {
                showImagePickerDialog()
            }

            binding.registerButton.setOnClickListener {
                if (validateInputs()) {
                    updateProfile()
                }
            }
        }

        private fun showProfileImage() {
            currentProfileImageUri?.let {
                binding.profileImageView.setImageURI(it)
            }
        }

        private fun showImagePickerDialog() {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            AlertDialog.Builder(this)
                .setTitle("Choose Profile Picture")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> startCamera()
                        1 -> startGallery()
                    }
                }
                .show()
        }

        private fun startCamera() {
            currentProfileImageUri = utils.getImageUri()
            launcherCamera.launch(currentProfileImageUri!!)
        }

        private fun startGallery() {
            launcherGallery.launch("image/*")
        }

        private fun validateInputs(): Boolean {
            val emptyFields = mutableListOf<String>()
            if (binding.bioEditText.text.toString().isEmpty()) emptyFields.add("bio")
            if (binding.jobEditText.text.toString().isEmpty()) emptyFields.add("job")
            if (binding.companyEditText.text.toString().isEmpty()) emptyFields.add("company")
            if (currentProfileImageUri == null) emptyFields.add("profile picture")

            return if (emptyFields.isEmpty()) {
                true
            } else {
                showToast("Please enter your ${emptyFields.joinToString(", ")}")
                false
            }
        }

    private fun updateProfile() {
        if (!validateInputs()) return

        showLoading()

        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val bioBody = binding.bioEditText.text.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val jobTitleBody = binding.jobEditText.text.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val companyBody = binding.companyEditText.text.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())

        val profilePicturePart = currentProfileImageUri?.let { uri ->
            val file = getFileFromURI(uri)?.let { resizeImage(it) }
            val requestFile = file?.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(
                "profile_picture",
                file?.name ?: "",
                requestFile!!
            )
        }

        if (profilePicturePart == null) {
            hideLoading()
            showToast("Error processing profile picture")
            return
        }

        ApiConfig.getApiService().editProfile(
            usernameBody,
            bioBody,
            jobTitleBody,
            companyBody,
            profilePicturePart,
            "Bearer $token"
        ).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                hideLoading()
                if (response.isSuccessful) {
                    showSuccessDialog()
                    uploadContacts()
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string() ?: "Unknown error occurred"
                    } catch (e: Exception) {
                        "Failed to update profile"
                    }
                    showToast(errorMessage)
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                hideLoading()
                showToast("Network error: ${t.message}")
            }
        })
    }

    private fun uploadContacts() {
        val contacts = contactAdapter.getContacts()
        var uploadedCount = 0

        contacts.forEach { contact ->
            ApiConfig.getApiService().addContactInfo(
                ApiService.ContactInfoRequest(
                    contact_type = contact.contact_type,
                    contact_value = contact.contact_value,
                    notes = contact.notes
                ),
                "Bearer $token"
            ).enqueue(object : Callback<ContactResponse> {
                override fun onResponse(call: Call<ContactResponse>, response: Response<ContactResponse>) {
                    uploadedCount++
                    if (uploadedCount == contacts.size) {
                        showSuccessDialog()
                    }
                }

                override fun onFailure(call: Call<ContactResponse>, t: Throwable) {
                    uploadedCount++
                    if (uploadedCount == contacts.size) {
                        showSuccessDialog()
                    }
                }
            })
        }

        // If no contacts to upload, show success dialog directly
        if (contacts.isEmpty()) {
            showSuccessDialog()
        }
    }

    private fun showLoading() {
        binding.loadingCard.visibility = View.VISIBLE
        binding.registerButton.isEnabled = false
        binding.changeProfileButton.isEnabled = false
    }

    private fun hideLoading() {
        binding.loadingCard.visibility = View.GONE
        binding.registerButton.isEnabled = true
        binding.changeProfileButton.isEnabled = true
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogRegistrationSuccessBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        dialogBinding.btnNavigateLogin.setOnClickListener {
            getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("user_token", token)
                .apply()

            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            dialog.dismiss()
            finish()
        }

        dialog.setCancelable(false)
        dialog.show()
    }




        private fun resizeImage(file: File): File {
            try {
                val exif = ExifInterface(file.absolutePath)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )

                val bitmap = BitmapFactory.decodeFile(file.path)
                val targetWidth = 1280
                val targetHeight = (bitmap.height * (targetWidth.toFloat() / bitmap.width)).toInt()

                val resizedBitmap =
                    Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                val resizedFile = File.createTempFile("resized_", ".jpg", cacheDir)

                FileOutputStream(resizedFile).use { out ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                val newExif = ExifInterface(resizedFile.absolutePath)
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                newExif.saveAttributes()

                return resizedFile
            } catch (e: Exception) {
                Log.e("ImageError", "Error resizing image", e)
                return file
            }
        }

        private fun getFileFromURI(uri: Uri): File? {
            return try {
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("temp_", ".jpg", cacheDir)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } catch (e: Exception) {
                Log.e("FileError", "Error processing file", e)
                null
            }
        }

        private fun showToast(message: String) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
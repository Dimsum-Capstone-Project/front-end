package com.example.dimsumproject.ui.register

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dimsumproject.databinding.ActivityRegister3Binding
import com.example.dimsumproject.MainActivity
import com.example.dimsumproject.Utils
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.RegisterResponse
import com.example.dimsumproject.data.local.UserPreferences
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
    private lateinit var userPreferences: UserPreferences
    private var palmImageUri: Uri? = null
    private var currentProfileImageUri: Uri? = null
    private lateinit var utils: Utils

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

        userPreferences = UserPreferences(this)
        utils = Utils(applicationContext)

        val username = intent.getStringExtra("username")
        val email = intent.getStringExtra("email")
        val password = intent.getStringExtra("password")
        val palmImageUriString = intent.getStringExtra("palmImageUri")
        palmImageUri = palmImageUriString?.let { Uri.parse(it) }

        binding.changeProfileButton.setOnClickListener {
            showImagePickerDialog()
        }

        binding.registerButton.setOnClickListener {
            if (validateInputs()) {
                userPreferences.saveUserProfile(
                    fullname = binding.fullnameEditText.text.toString(),
                    job = binding.jobEditText.text.toString(),
                    company = binding.companyEditText.text.toString(),
                    instagram = binding.instagramEditText.text.toString(),
                    linkedin = binding.linkedinEditText.text.toString(),
                    whatsapp = binding.whatsappEditText.text.toString(),
                    profileImageUri = currentProfileImageUri
                )

                registerUser(username, email, password)
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
        if (binding.fullnameEditText.text.toString().isEmpty()) {
            showToast("Please enter your full name")
            return false
        }
        if (binding.jobEditText.text.toString().isEmpty()) {
            showToast("Please enter your job")
            return false
        }
        if (binding.companyEditText.text.toString().isEmpty()) {
            showToast("Please enter your company")
            return false
        }
        return true
    }

    private fun resizeImage(file: File): File {
        try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            // Decode gambar untuk mendapatkan dimensi asli
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            val targetWidth = 1280
            val targetHeight: Int

            // Jika gambar portrait (tinggi > lebar)
            if (originalHeight > originalWidth) {
                targetHeight = (originalHeight.toFloat() * (targetWidth.toFloat() / originalWidth.toFloat())).toInt()
            } else {
                targetHeight = (originalHeight.toFloat() * (targetWidth.toFloat() / originalWidth.toFloat())).toInt()
            }

            options.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeFile(file.path)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)


            val resizedFile = File.createTempFile("resized_", ".jpg", cacheDir)
            FileOutputStream(resizedFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            val newExif = ExifInterface(resizedFile.absolutePath)
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            newExif.saveAttributes()

            Log.d("ImageDebug", "Original dimensions: ${originalWidth}x${originalHeight}")
            Log.d("ImageDebug", "Resized dimensions: ${targetWidth}x${targetHeight}")
            Log.d("ImageDebug", "Original size: ${file.length()} bytes")
            Log.d("ImageDebug", "Resized size: ${resizedFile.length()} bytes")

            return resizedFile
        } catch (e: Exception) {
            Log.e("ImageError", "Error resizing image", e)
            return file
        }
    }

    private fun registerUser(username: String?, email: String?, password: String?) {
        if (username == null || email == null || password == null) {
            showToast("Required fields are missing")
            return
        }

        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val passwordBody = password.toRequestBody("text/plain".toMediaTypeOrNull())

        val palmImageFile = getFileFromURI(palmImageUri!!)
        if (palmImageFile == null || !palmImageFile.exists()) {
            showToast("Palm image file not found")
            return
        }

        Log.d("ImageDebug", "File size before sending: ${palmImageFile.length()} bytes")

        val resizedFile = resizeImage(palmImageFile)

        val requestFilePalm = resizedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val palmImagePart = MultipartBody.Part.createFormData("palm_image", resizedFile.name, requestFilePalm)

        ApiConfig.getApiService().registerUser(
            palmImagePart,
            emailBody,
            usernameBody,
            passwordBody
        ).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(
                call: Call<RegisterResponse>,
                response: Response<RegisterResponse>
            ) {
                if (response.isSuccessful) {
                    showToast("Registration successful!")
                    startActivity(Intent(this@Register3Activity, MainActivity::class.java))
                    finishAffinity()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("RegisterError", "Error Body: $errorBody")
                    showToast("Registration failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("RegisterError", "Error: ${t.message}", t)
                showToast("Error: ${t.message}")
            }
        })
    }

    private fun getFileFromURI(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Deteksi dimensi gambar
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(tempFile.path, options)
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            Log.d("ImageDebug", "Original File Size: ${tempFile.length()} bytes")
            Log.d("ImageDebug", "Image Dimensions: ${imageWidth}x${imageHeight} pixels")

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
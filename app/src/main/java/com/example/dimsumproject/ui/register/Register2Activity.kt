package com.example.dimsumproject.ui.register

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dimsumproject.Utils
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.ApiService
import com.example.dimsumproject.data.api.LoginResponse
import com.example.dimsumproject.data.api.RegisterResponse
import com.example.dimsumproject.databinding.ActivityRegister2Binding
import com.example.dimsumproject.databinding.DialogRegistrationErrorBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class Register2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityRegister2Binding
    private var currentImageUri: Uri? = null
    private lateinit var utils: Utils

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        } else {
            currentImageUri = null
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentImageUri = uri
            binding.uploadImageView.setImageURI(uri)
        }
    }

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        utils = Utils(applicationContext)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardUploadImage.setOnClickListener {
            showImageUploadOptions()
        }

        binding.nextButton.setOnClickListener {
            if (currentImageUri != null) {
                registerAndLogin()
            } else {
                showToast("Please upload palm image")
            }
        }
    }

    private fun registerAndLogin() {
        val username = intent.getStringExtra("username")
        val email = intent.getStringExtra("email")
        val password = intent.getStringExtra("password")

        if (username == null || email == null || password == null) {
            showToast("Required data missing")
            return
        }

        val palmImageFile = getFileFromURI(currentImageUri!!)
        if (palmImageFile == null) {
            showToast("Error processing image")
            return
        }

        val resizedFile = resizeImage(palmImageFile)
        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val passwordBody = password.toRequestBody("text/plain".toMediaTypeOrNull())
        val requestFilePalm = resizedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val palmImagePart = MultipartBody.Part.createFormData("palm_image", resizedFile.name, requestFilePalm)

        showLoading()

        ApiConfig.getApiService().registerUser(
            palmImagePart,
            emailBody,
            usernameBody,
            passwordBody
        ).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    loginUser(email, password)
                } else {
                    hideLoading()
                    handleRegistrationError(response)
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                val errorResponse = Response.error<RegisterResponse>(
                    500,
                    okhttp3.ResponseBody.create(
                        "text/plain".toMediaTypeOrNull(),
                        "Network error: Unable to connect to server"
                    )
                )
                handleRegistrationError(errorResponse)
            }
        })
    }

    private fun loginUser(email: String, password: String) {
        val loginRequest = ApiService.LoginRequest(email, password)

        ApiConfig.getApiService().loginUser(loginRequest)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    hideLoading()
                    if (response.isSuccessful) {
                        response.body()?.access_token?.let { token ->
                            saveTokenAndNavigate(token)
                        } ?: showToast("Invalid login response")
                    } else {
                        showToast("Login failed")
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    hideLoading()
                    showToast("Login error: ${t.message}")
                }
            })
    }

    private fun handleRegistrationError(response: Response<RegisterResponse>) {
        val dialog = Dialog(this)
        val dialogBinding = DialogRegistrationErrorBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Set dialog window properties
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        // Set error message based on the error type
        val errorBody = response.errorBody()?.string()
        val errorMessage = when {
            errorBody?.contains("palm image") == true ->
                "Your palm image couldn't be processed. Please ensure you take a clear photo with good lighting and try again."
            errorBody?.contains("email format") == true ->
                "Please enter a valid email address."
            errorBody?.contains("already exists") == true ->
                "This email is already registered. Please use a different email."
            else -> "The palm is already registered. Please try again."
        }
        dialogBinding.tvMessage.text = errorMessage

        // Handle try again button click
        dialogBinding.btnTryAgain.setOnClickListener {
            dialog.dismiss()
            // Clear the current palm image
            currentImageUri = null
            binding.uploadImageView.setImageResource(android.R.drawable.ic_menu_camera)
            showImageUploadOptions()
        }

        // Make dialog cancellable
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun saveTokenAndNavigate(token: String) {
        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("user_token", token)
            .apply()

        val intent = Intent(this, Register3Activity::class.java).apply {
            putExtra("token", token)
            putExtra("username", intent.getStringExtra("username")) // Tambahkan username
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
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

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
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

    private fun showImageUploadOptions() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Upload Palm Image")
        builder.setMessage(
            "Please ensure:\n" +
                    "• Take a clear photo of your palm\n" +
                    "• Your entire palm should be visible\n" +
                    "• Good lighting condition\n" +
                    "• Palm should face the camera directly\n" +
                    "• Avoid blur or dark images"
        )
        builder.setPositiveButton("Take Photo") { _, _ ->
            checkPermissionsAndStartCamera()
        }
        builder.setNegativeButton("Choose from Gallery") { _, _ ->
            openGallery()
        }
        builder.show()
    }

    private fun checkPermissionsAndStartCamera() {
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

    private fun openGallery() {
        launcherIntentGallery.launch("image/*")
    }

    private fun showImage() {
        currentImageUri?.let {
            binding.uploadImageView.setImageURI(it)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                showToast("Camera permission required")
            }
        }
    }
    private fun showLoading() {
        binding.loadingCard.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingCard.visibility = View.GONE
    }
}
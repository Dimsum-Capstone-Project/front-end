package com.example.dimsumproject.ui.register

import android.app.Dialog
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
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dimsumproject.databinding.ActivityRegister3Binding
import com.example.dimsumproject.Utils
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.ProfileResponse
import com.example.dimsumproject.data.api.RegisterResponse
import com.example.dimsumproject.databinding.DialogRegistrationErrorBinding
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

        utils = Utils(applicationContext)

        val username = intent.getStringExtra("username")
        val email = intent.getStringExtra("email")
        val password = intent.getStringExtra("password")
        val palmImageUriString = intent.getStringExtra("palmImageUri")
        palmImageUri = palmImageUriString?.let { Uri.parse(it) }


        binding.fullnameEditText.setText(intent.getStringExtra("fullname"))
        binding.bioEditText.setText(intent.getStringExtra("bio"))
        binding.jobEditText.setText(intent.getStringExtra("job"))
        binding.companyEditText.setText(intent.getStringExtra("company"))


        binding.changeProfileButton.setOnClickListener {
            showImagePickerDialog()
        }

        binding.registerButton.setOnClickListener {
            if (validateInputs()) {
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
        if (binding.bioEditText.text.toString().isEmpty()) {
            showToast("Please enter your bio")
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

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            val targetWidth = 1280
            val targetHeight = if (originalHeight > originalWidth) {
                (originalHeight.toFloat() * (targetWidth.toFloat() / originalWidth.toFloat())).toInt()
            } else {
                (originalHeight.toFloat() * (targetWidth.toFloat() / originalWidth.toFloat())).toInt()
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
                    showSuccessDialog()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("RegisterError", "Error Body: $errorBody")

                    // Parse error message and determine error type
                    val errorMessage = when {
                        errorBody?.contains("palm image") == true -> {
                            "Your palm image couldn't be processed. Please try taking a new photo."
                        }
                        errorBody?.contains("email format") == true -> {
                            "Please check your email format and try again."
                        }
                        errorBody?.contains("already exists") == true -> {
                            "This email is already registered. Please use a different email."
                        }
                        else -> {
                            "Something went wrong. Please try again."
                        }
                    }

                    // Determine error type for navigation
                    val errorType = when {
                        errorBody?.contains("palm image") == true -> ErrorType.PALM_IMAGE
                        else -> ErrorType.REGISTRATION_FORM
                    }
                    showErrorDialog(errorMessage, errorType)
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("RegisterError", "Error: ${t.message}", t)
                showErrorDialog(
                    "Connection error. Please check your internet connection and try again.",
                    ErrorType.REGISTRATION_FORM
                )
            }
        })
    }

    private fun editUserProfile() {
        val nameBody = binding.fullnameEditText.text.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val bioBody = binding.bioEditText.text.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val jobTitleBody = binding.jobEditText.text.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())
        val companyBody = binding.companyEditText.text.toString()
            .toRequestBody("text/plain".toMediaTypeOrNull())

        val profilePicturePart = currentProfileImageUri?.let { uri ->
            val file = getFileFromURI(uri)
            val requestFile = file?.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("profile_picture", file?.name ?: "", requestFile!!)
        }

        if (profilePicturePart == null) {
            showToast("Profile picture is required")
            return
        }

        ApiConfig.getApiService().editProfile(
            nameBody,
            bioBody,
            jobTitleBody,
            companyBody,
            profilePicturePart,
            "Bearer token disini"
        ).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                if (response.isSuccessful) {
                    showSuccessDialog()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ProfileError", "Error Body: $errorBody")
                    showErrorDialog(
                        "Failed to update profile. Please try again.",
                        ErrorType.REGISTRATION_FORM
                    )
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Log.e("ProfileError", "Error: ${t.message}", t)
                showErrorDialog(
                    "Connection error. Please check your internet connection and try again.",
                    ErrorType.REGISTRATION_FORM
                )
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

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val bindingDialog = DialogRegistrationSuccessBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)


        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        bindingDialog.btnLogin.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showErrorDialog(errorMessage: String, errorType: ErrorType) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val bindingDialog = DialogRegistrationErrorBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        bindingDialog.tvMessage.text = errorMessage
        bindingDialog.btnTryAgain.setOnClickListener {
            dialog.dismiss()
            when (errorType) {
                ErrorType.PALM_IMAGE -> {
                    val intent = Intent(this, Register2Activity::class.java)
                    intent.putExtra("username", getIntent().getStringExtra("username"))
                    intent.putExtra("email", getIntent().getStringExtra("email"))
                    intent.putExtra("password", getIntent().getStringExtra("password"))
                    intent.putExtra("fullname", binding.fullnameEditText.text.toString())
                    intent.putExtra("bio", binding.bioEditText.text.toString())
                    intent.putExtra("job", binding.jobEditText.text.toString())
                    intent.putExtra("company", binding.companyEditText.text.toString())


                    startActivity(intent)
                    finish()
                }
                ErrorType.REGISTRATION_FORM -> {
                    dialog.dismiss()
                }
            }
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    enum class ErrorType {
        PALM_IMAGE,
        REGISTRATION_FORM
    }
}
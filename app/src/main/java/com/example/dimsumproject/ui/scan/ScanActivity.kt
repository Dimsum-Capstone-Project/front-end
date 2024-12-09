package com.example.dimsumproject.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.dimsumproject.R
import com.example.dimsumproject.databinding.ActivityScanBinding
import com.example.dimsumproject.ui.home.ContactsAdapter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private val viewModel: ScanViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        processIncomingImage()
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(emptyList())
        binding.rvContacts.apply {
            layoutManager = GridLayoutManager(this@ScanActivity, 2)
            adapter = contactsAdapter
        }
    }


    private fun setupObservers() {
        viewModel.profile.observe(this) { profile ->
            with(binding) {
                profileContainer.visibility = View.VISIBLE

                Glide.with(this@ScanActivity)
                    .load(profile.profile_picture)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfile)

                tvUsername.text = profile.username
                tvBio.text = profile.bio ?: "No bio added"
                tvJobTitle.text = profile.job_title ?: "No job title"
            }
        }

        viewModel.contacts.observe(this) { contactResponse ->
            binding.rvContacts.adapter = ContactsAdapter(contactResponse.contacts)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            if (!isFinishing && error.isNotEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun processIncomingImage() {
        val imageUriString = intent.getStringExtra("image_uri") ?: run {
            Log.e("ImageProcessing", "No image URI provided")
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            Log.d("ImageProcessing", "Received image URI: $imageUriString")
            val imageUri = Uri.parse(imageUriString)

            val file = getFileFromUri(imageUri)
            if (file == null) {
                Log.e("ImageProcessing", "Failed to get file from URI")
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            Log.d("ImageProcessing", "Original file path: ${file.absolutePath}")

            val resizedFile = resizeImage(file)
            val token = getStoredToken()

            if (token.isEmpty()) {
                Log.e("ImageProcessing", "Empty token")
                Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val requestFile = resizedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "palm_image",
                "palm_image.jpg",
                requestFile
            )

            Log.d("ImageProcessing", "Sending image to server...")
            viewModel.recognizePalm(imagePart, token)
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error in processIncomingImage", e)
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun resizeImage(file: File): File {
        try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            Log.d("ImageProcessing", "Original file size: ${file.length() / 1024} KB")

            val bitmap = BitmapFactory.decodeFile(file.path)
            Log.d("ImageProcessing", "Original resolution: ${bitmap.width}x${bitmap.height}")

            // Hanya resize jika gambar lebih besar dari target
            val targetWidth = 1280
            val finalBitmap = if (bitmap.width > targetWidth) {
                val targetHeight = (bitmap.height * (targetWidth.toFloat() / bitmap.width)).toInt()
                Log.d("ImageProcessing", "Resizing to: ${targetWidth}x${targetHeight}")
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                Log.d("ImageProcessing", "Keeping original size: ${bitmap.width}x${bitmap.height}")
                bitmap
            }

            val resizedFile = File.createTempFile("resized_", ".jpg", cacheDir)

            FileOutputStream(resizedFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.flush()
            }

            Log.d("ImageProcessing", "Final file size: ${resizedFile.length() / 1024} KB")
            Log.d("ImageProcessing", "Final resolution: ${finalBitmap.width}x${finalBitmap.height}")

            val newExif = ExifInterface(resizedFile.absolutePath)
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            newExif.saveAttributes()

            if (finalBitmap !== bitmap) {
                finalBitmap.recycle()
            }
            bitmap.recycle()

            return resizedFile
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error resizing image", e)
            Log.e("ImageProcessing", "Error details: ${e.message}")
            e.printStackTrace()
            return file
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
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
            Log.e("ScanActivity", "Error getting file from URI", e)
            null
        }
    }

    private fun getStoredToken(): String {
        return getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("access_token", "") ?: ""
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            binding.ivProfile.setImageDrawable(null)
        } catch (e: Exception) {
            Log.e("ScanActivity", "Error cleaning up resources", e)
        }
    }
}
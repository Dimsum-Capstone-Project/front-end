package com.example.dimsumproject.ui.register

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dimsumproject.Utils
import com.example.dimsumproject.databinding.ActivityRegister2Binding

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

    private val CAMERA_PERMISSION_REQUEST_CODE= 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        utils = Utils(applicationContext)

        binding.cardUploadImage.setOnClickListener {
            showImageUploadOptions()
        }

        binding.nextButton.setOnClickListener {
            if (currentImageUri != null) {
                navigateToRegister3()
            } else {
                showToast("Please upload palm image")
            }
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

    private fun navigateToRegister3() {
        val intent = Intent(this, Register3Activity::class.java)
        intent.putExtra("username", getIntent().getStringExtra("username"))
        intent.putExtra("email", getIntent().getStringExtra("email"))
        intent.putExtra("password", getIntent().getStringExtra("password"))
        intent.putExtra("palmImageUri", currentImageUri.toString())
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
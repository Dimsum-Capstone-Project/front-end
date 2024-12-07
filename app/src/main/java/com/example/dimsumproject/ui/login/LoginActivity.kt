package com.example.dimsumproject.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.ApiService
import com.example.dimsumproject.data.api.LoginResponse
import com.example.dimsumproject.databinding.ActivityLoginBinding
import com.example.dimsumproject.ui.home.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = ApiConfig.getApiService()

        // Check if user is already logged in
        if (checkAccessToken()) {
            navigateToHome()
            return
        }

        setupActions()
    }

    private fun setupActions() {
        binding.login.setOnClickListener {
            val email = binding.username.text.toString()
            val password = binding.password.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showNotification("Please fill in all fields")
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                showNotification("Invalid email format")
                return@setOnClickListener
            }

            if (password.length < 8) {
                showNotification("Password must be at least 8 characters")
                return@setOnClickListener
            }

            // Show loading before making API call
            showLoading()
            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        val loginRequest = ApiService.LoginRequest(email, password)

        apiService.loginUser(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                hideLoading()
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    loginResponse?.let {
                        // Simpan access token ke SharedPreferences
                        saveAccessToken(it.access_token)

                        // Navigasi ke HomeActivity
                        navigateToHome()
                    }
                } else {
                    showNotification("Incorrect email or password")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                hideLoading()
                showNotification("Login failed. Please try again.")
            }
        })
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

    private fun saveAccessToken(accessToken: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("access_token", accessToken)
        editor.apply()
    }

    private fun checkAccessToken(): Boolean {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("access_token", null)
        return !token.isNullOrEmpty()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            // Tambahkan flags agar user tidak bisa kembali ke LoginActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showNotification(message: String) {
        val notificationLayout = binding.notificationLayout
        val tvNotification = binding.tvNotification

        tvNotification.text = message
        notificationLayout.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            notificationLayout.visibility = View.GONE
        }, 3000)
    }
}
package com.example.dimsumproject.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
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
    private val notificationHandler = Handler(Looper.getMainLooper())
    private val notificationRunnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            binding.notificationLayout.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = ApiConfig.getApiService()

        setupActions()
    }

    private fun setupActions() {
        binding.login.setOnClickListener {
            if (!isFinishing && !isDestroyed) {
                val email = binding.username.text.toString()
                val password = binding.password.text.toString()

                when {
                    email.isEmpty() || password.isEmpty() -> {
                        showNotification("Please fill in all fields")
                    }
                    !isValidEmail(email) -> {
                        showNotification("Invalid email format")
                    }
                    password.length < 8 -> {
                        showNotification("Password must be at least 8 characters")
                    }
                    else -> {
                        binding.login.isEnabled = false  // Disable button while processing
                        loginUser(email, password)
                    }
                }
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        val loginRequest = ApiService.LoginRequest(email, password)

        apiService.loginUser(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (!isFinishing && !isDestroyed) {
                    binding.login.isEnabled = true  // Re-enable button

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        loginResponse?.let {
                            // Remove any pending notification dismissal
                            notificationHandler.removeCallbacks(notificationRunnable)

                            // Save access token
                            saveAccessToken(it.access_token)

                            // Navigate to HomeActivity
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        showNotification("Incorrect email or password")
                    }
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                if (!isFinishing && !isDestroyed) {
                    binding.login.isEnabled = true  // Re-enable button
                    showNotification("Login failed. Please try again.")
                }
            }
        })
    }

    private fun saveAccessToken(accessToken: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("access_token", accessToken)
            apply()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showNotification(message: String) {
        if (!isFinishing && !isDestroyed) {
            // Remove any existing notification dismissal callback
            notificationHandler.removeCallbacks(notificationRunnable)

            binding.apply {
                tvNotification.text = message
                notificationLayout.visibility = View.VISIBLE
            }

            // Schedule the notification to be hidden
            notificationHandler.postDelayed(notificationRunnable, 3000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to prevent memory leaks
        notificationHandler.removeCallbacks(notificationRunnable)
    }
}
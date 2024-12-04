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

            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        val loginRequest = ApiService.LoginRequest(email, password)

        apiService.loginUser(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    loginResponse?.let {
                        // Simpan access token ke SharedPreferences
                        saveAccessToken(it.access_token)

                        // Navigasi ke HomeActivity
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    showNotification("Incorrect email or password")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showNotification("Login failed. Please try again.")
            }
        })
    }

    private fun saveAccessToken(accessToken: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("access_token", accessToken)
        editor.apply()
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
package com.example.dimsumproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dimsumproject.databinding.ActivityMainBinding
import com.example.dimsumproject.ui.home.HomeActivity
import com.example.dimsumproject.ui.login.LoginActivity
import com.example.dimsumproject.ui.register.Register1Activity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Periksa apakah pengguna sudah login atau memiliki token yang tersimpan
        if (isUserLoggedIn()) {
            // Jika pengguna sudah login, langsung arahkan ke HomeActivity
            navigateToHomeActivity()
        } else {
            // Jika pengguna belum login, tampilkan pilihan login atau signup
            setupActions()
        }
    }

    private fun setupActions() {
        binding.btnLogin.setOnClickListener {
            // Menggunakan path yang benar ke LoginActivity dalam package ui.login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnSignup.setOnClickListener {
            // Pastikan path Register1Activity sesuai dengan structure folder Anda
            startActivity(Intent(this, Register1Activity::class.java))
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        return accessToken != null
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
package com.example.dimsumproject.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dimsumproject.databinding.ActivityRegister1Binding


class Register1Activity : AppCompatActivity() {
    private lateinit var binding: ActivityRegister1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegister1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nextButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (validateInputs(username, email, password)) {
                val intent = Intent(this, Register2Activity::class.java)
                intent.putExtra("username", username)
                intent.putExtra("email", email)
                intent.putExtra("password", password)
                startActivity(intent)
            }
        }
    }

    private fun validateInputs(username: String, email: String, password: String): Boolean {
        when {
            username.isEmpty() -> {
                showToast("Please enter username")
                return false
            }
            email.isEmpty() -> {
                showToast("Please enter email")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("Please enter valid email")
                return false
            }
            password.isEmpty() -> {
                showToast("Please enter password")
                return false
            }
            password.length < 8 -> {
                showToast("Password must be at least 8 characters")
                return false
            }
        }
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
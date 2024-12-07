package com.example.dimsumproject.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.ContactResponse
import com.example.dimsumproject.data.api.ProfileResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _profile = MutableLiveData<ProfileResponse>()
    val profile: LiveData<ProfileResponse> = _profile

    private val _contacts = MutableLiveData<ContactResponse>()
    val contacts: LiveData<ContactResponse> = _contacts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    private fun getAuthToken(): String {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
            "MyPrefs",
            Context.MODE_PRIVATE
        )
        val token = sharedPreferences.getString("access_token", "") ?: ""
        return "Bearer $token"
    }

    fun loadProfile() {
        if (_isLoading.value == true) return // Prevent multiple simultaneous loads

        _isLoading.value = true
        val client = ApiConfig.getApiService()

        client.getProfile(getAuthToken()).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    response.body()?.let {
                        _profile.value = it
                    }
                } else {
                    handleErrorResponse(response)
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Network error: ${t.message}"
            }
        })
    }

    fun loadContacts() {
        if (_isLoading.value == true) return // Prevent multiple simultaneous loads

        val client = ApiConfig.getApiService()

        client.getContactInfo(getAuthToken()).enqueue(object : Callback<ContactResponse> {
            override fun onResponse(call: Call<ContactResponse>, response: Response<ContactResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    response.body()?.let {
                        _contacts.value = it
                    }
                } else {
                    handleErrorResponse(response)
                }
            }

            override fun onFailure(call: Call<ContactResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Network error: ${t.message}"
            }
        })
    }

    private fun handleErrorResponse(response: Response<*>) {
        when (response.code()) {
            401, 403 -> {
                _error.value = response.code().toString()
                handleAuthError()
            }
            else -> {
                _error.value = "Error: ${response.code()} - ${response.message()}"
            }
        }
    }

    private fun handleAuthError() {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
            "MyPrefs",
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().remove("access_token").apply()
        _navigateToLogin.value = true
    }
}
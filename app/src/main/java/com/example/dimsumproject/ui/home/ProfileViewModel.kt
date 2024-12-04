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
        _isLoading.value = true
        val client = ApiConfig.getApiService()

        client.getProfile(getAuthToken()).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _profile.value = response.body()
                } else if (response.code() == 403) {
                    handleAuthError()
                } else {
                    _error.value = "Failed to load profile"
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = t.message ?: "Unknown error occurred"
            }
        })
    }

    fun loadContacts() {
        val client = ApiConfig.getApiService()

        client.getContactInfo(getAuthToken()).enqueue(object : Callback<ContactResponse> {
            override fun onResponse(call: Call<ContactResponse>, response: Response<ContactResponse>) {
                if (response.isSuccessful) {
                    _contacts.value = response.body()
                } else if (response.code() == 403) {
                    handleAuthError()
                }
            }

            override fun onFailure(call: Call<ContactResponse>, t: Throwable) {
                _error.value = t.message ?: "Unknown error occurred"
            }
        })
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
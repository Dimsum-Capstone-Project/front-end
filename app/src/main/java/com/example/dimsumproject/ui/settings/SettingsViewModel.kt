package com.example.dimsumproject.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.ProfileResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _profile = MutableLiveData<ProfileResponse>()
    val profile: LiveData<ProfileResponse> = _profile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<ProfileUpdateError>()
    val error: LiveData<ProfileUpdateError> = _error

    private val _loadingTimeout = MutableLiveData<Boolean>()
    val loadingTimeout: LiveData<Boolean> = _loadingTimeout

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    private var loadingJob: Job? = null

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
        startLoadingTimeout()

        val client = ApiConfig.getApiService()
        client.getProfile(getAuthToken()).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                _isLoading.value = false
                cancelLoadingTimeout()

                if (response.isSuccessful) {
                    response.body()?.let {
                        _profile.value = it
                    }
                } else {
                    when (response.code()) {
                        401, 403 -> handleAuthError()
                        else -> _error.value = ProfileUpdateError.ServerError
                    }
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                _isLoading.value = false
                cancelLoadingTimeout()
                _error.value = ProfileUpdateError.NetworkError
            }
        })
    }

    fun updateProfile(
        username: String,
        bio: String,
        company: String,
        jobTitle: String,
        profilePicture: MultipartBody.Part?
    ) {
        if (profilePicture != null) {
            updateProfileWithImage(username, bio, company, jobTitle, profilePicture)
        } else {
            updateProfileWithoutImage(username, bio, company, jobTitle)
        }
    }

    private fun updateProfileWithImage(
        username: String,
        bio: String,
        company: String,
        jobTitle: String,
        profilePicture: MultipartBody.Part
    ) {
        _isLoading.value = true
        startLoadingTimeout()

        val client = ApiConfig.getApiService()
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
        val companyBody = company.toRequestBody("text/plain".toMediaTypeOrNull())
        val jobTitleBody = jobTitle.toRequestBody("text/plain".toMediaTypeOrNull())

        client.editProfile(
            usernameBody,
            bioBody,
            jobTitleBody,
            companyBody,
            profilePicture,
            getAuthToken()
        ).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                handleUpdateResponse(response)
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                handleUpdateFailure()
            }
        })
    }

    private fun updateProfileWithoutImage(
        username: String,
        bio: String,
        company: String,
        jobTitle: String
    ) {
        _isLoading.value = true
        startLoadingTimeout()

        val client = ApiConfig.getApiService()
        val usernameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())
        val companyBody = company.toRequestBody("text/plain".toMediaTypeOrNull())
        val jobTitleBody = jobTitle.toRequestBody("text/plain".toMediaTypeOrNull())

        client.editProfileWithoutImage(
            usernameBody,
            bioBody,
            jobTitleBody,
            companyBody,
            getAuthToken()
        ).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                handleUpdateResponse(response)
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                handleUpdateFailure()
            }
        })
    }

    private fun handleUpdateResponse(response: Response<ProfileResponse>) {
        _isLoading.value = false
        cancelLoadingTimeout()

        if (response.isSuccessful) {
            response.body()?.let {
                _profile.value = it
            }
        } else {
            when (response.code()) {
                401, 403 -> handleAuthError()
                422 -> _error.value = ProfileUpdateError.ValidationError(
                    "update",
                    "Invalid input data"
                )
                else -> _error.value = ProfileUpdateError.ServerError
            }
        }
    }

    private fun handleUpdateFailure() {
        _isLoading.value = false
        cancelLoadingTimeout()
        _error.value = ProfileUpdateError.NetworkError
    }

    private fun startLoadingTimeout() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            delay(5000)
            _loadingTimeout.value = true
        }
    }

    private fun cancelLoadingTimeout() {
        loadingJob?.cancel()
        _loadingTimeout.value = false
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
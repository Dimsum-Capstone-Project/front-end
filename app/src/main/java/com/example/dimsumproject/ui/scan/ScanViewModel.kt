package com.example.dimsumproject.ui.scan

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.Contact
import com.example.dimsumproject.data.api.ContactResponse
import com.example.dimsumproject.data.api.HistoryResponse
import com.example.dimsumproject.data.api.ProfileResponse
import com.example.dimsumproject.data.api.RecognizePalmResponse
import com.google.gson.Gson
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScanViewModel : ViewModel() {
    private val _profile = MutableLiveData<ProfileResponse>()
    val profile: LiveData<ProfileResponse> = _profile

    private val _contacts = MutableLiveData<ContactResponse>()
    val contacts: LiveData<ContactResponse> = _contacts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun recognizePalm(palm_image: MultipartBody.Part, token: String) {
        _isLoading.value = true
        _error.value = ""

        val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"

        ApiConfig.getApiService().recognizePalm(palm_image, authToken)
            .enqueue(object : Callback<RecognizePalmResponse> {
                override fun onResponse(
                    call: Call<RecognizePalmResponse>,
                    response: Response<RecognizePalmResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { recognizeResponse ->
                            Log.d("RecognizePalm", "Raw Response: $response")
                            Log.d("RecognizePalm", "User username: ${recognizeResponse.user.username}")
                            Log.d("RecognizePalm", "Profile data: ${recognizeResponse.profile}")

                            val completeProfile = recognizeResponse.profile.copy(
                                email = recognizeResponse.user.email,
                                username = recognizeResponse.user.username
                            )
                            _profile.value = completeProfile
                            getHistoryForContact(authToken, recognizeResponse.user.email)
                        } ?: run {
                            _isLoading.value = false
                            _error.value = "Empty response from server"
                        }
                    } else {
                        _isLoading.value = false
                        val errorMsg = when (response.code()) {
                            401 -> "Authentication failed"
                            403 -> "Access denied"
                            404 -> "Palm not found in database"
                            422 -> {
                                val errorBody = response.errorBody()?.string()
                                try {
                                    val error = Gson().fromJson(errorBody, ValidationError::class.java)
                                    error.detail[0].msg
                                } catch (e: Exception) {
                                    "Validation failed"
                                }
                            }
                            else -> "Recognition failed (${response.code()}): ${response.message()}"
                        }
                        _error.value = errorMsg
                    }
                }

                override fun onFailure(call: Call<RecognizePalmResponse>, t: Throwable) {
                    _isLoading.value = false
                    _error.value = "Network error: ${t.message}"
                    Log.e("RecognizePalm", "Network error", t)
                }
            })
    }

    private fun getHistoryForContact(token: String, userEmail: String) {
        ApiConfig.getApiService().getHistory(token)
            .enqueue(object : Callback<HistoryResponse> {
                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        response.body()?.let { historyResponse ->
                            Log.d("History", "Full Response: $historyResponse")

                            val historyItem = historyResponse.who_i_scanned
                                .maxByOrNull { it.time_scanned }

                            Log.d("History", "Found history item: $historyItem")

                            historyItem?.contacts?.let { historyContacts ->
                                val contactResponse = ContactResponse(
                                    contacts = historyContacts.map { historyContact ->
                                        Contact(
                                            contact_id = historyContact.contact_id,
                                            contact_type = historyContact.contact_type,
                                            contact_value = historyContact.contact_value,
                                            notes = historyContact.notes
                                        )
                                    }
                                )
                                Log.d("History", "Setting contacts: $contactResponse")
                                _contacts.value = contactResponse
                            }
                        }
                    } else {
                        Log.e("History", "Response not successful: ${response.code()}")
                        _error.value = "Failed to fetch history"
                    }
                }

                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    _isLoading.value = false
                    _error.value = "Network error while fetching history: ${t.message}"
                    Log.e("RecognizePalm", "History fetch error", t)
                }
            })
    }
}

data class ValidationError(
    val detail: List<ErrorDetail>
)

data class ErrorDetail(
    val loc: List<String>,
    val msg: String,
    val type: String
)
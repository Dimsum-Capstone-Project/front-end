package com.example.dimsumproject.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dimsumproject.data.api.AddContactRequest
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.Contact
import com.example.dimsumproject.data.api.ContactResponse
import com.example.dimsumproject.data.api.DeleteContactRequest
import com.example.dimsumproject.data.api.DeleteContactResponse
import com.example.dimsumproject.data.api.EditContactRequest
import com.example.dimsumproject.data.api.EditContactResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _success = MutableLiveData<String>()
    val success: LiveData<String> = _success

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private var retryCount = 0
    private val maxRetries = 3

    fun loadContacts(token: String) {
        _isLoading.value = true
        ApiConfig.getApiService().getContactInfo("Bearer $token")
            .enqueue(object : Callback<ContactResponse> {
                override fun onResponse(
                    call: Call<ContactResponse>,
                    response: Response<ContactResponse>
                ) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        response.body()?.let {
                            _contacts.value = it.contacts
                        }
                    } else if (response.code() == 404) {
                        // Handle "No contact information found for the user." as a valid state
                        val errorBody = response.errorBody()?.string()
                        if (errorBody?.contains("No contact information found") == true) {
                            _contacts.value = emptyList() // No contacts available
                        } else {
                            handleError(response)
                        }
                    } else {
                        handleError(response)
                    }
                }

                override fun onFailure(call: Call<ContactResponse>, t: Throwable) {
                    handleFailure(t, token) { loadContacts(token) }
                }
            })
    }

    fun addContact(type: String, value: String, notes: String, token: String) {
        _isLoading.value = true
        val request = AddContactRequest(type, value, notes)

        ApiConfig.getApiService().addContactInfoHome(request, "Bearer $token")
            .enqueue(object : Callback<ContactResponse> {
                override fun onResponse(call: Call<ContactResponse>, response: Response<ContactResponse>) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        _success.value = "Contact added successfully"
                        loadContacts(token)
                    } else {
                        handleError(response)
                    }
                }

                override fun onFailure(call: Call<ContactResponse>, t: Throwable) {
                    handleFailure(t, token) { addContact(type, value, notes, token) }
                }
            })
    }

    fun editContact(id: String, type: String, value: String, notes: String, token: String) {
        _isLoading.value = true
        val request = EditContactRequest(id, type, value, notes)

        ApiConfig.getApiService().editContactInfo(request, "Bearer $token")
            .enqueue(object : Callback<EditContactResponse> {
                override fun onResponse(
                    call: Call<EditContactResponse>,
                    response: Response<EditContactResponse>
                ) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        response.body()?.let {
                            _success.value = it.message
                            loadContacts(token)
                        }
                    } else {
                        handleError(response)
                    }
                }

                override fun onFailure(call: Call<EditContactResponse>, t: Throwable) {
                    handleFailure(t, token) { editContact(id, type, value, notes, token) }
                }
            })
    }

    fun deleteContact(id: String, token: String) {
        _isLoading.value = true
        val request = DeleteContactRequest(id)

        ApiConfig.getApiService().deleteContactInfo(request, "Bearer $token")
            .enqueue(object : Callback<DeleteContactResponse> {
                override fun onResponse(
                    call: Call<DeleteContactResponse>,
                    response: Response<DeleteContactResponse>
                ) {
                    _isLoading.value = false
                    loadContacts(token)
                    if (response.isSuccessful) {
                        response.body()?.let {
                            _success.value = it.message
                            loadContacts(token)
                        }
                    } else {
                        handleError(response)
                    }
                }

                override fun onFailure(call: Call<DeleteContactResponse>, t: Throwable) {
                    handleFailure(t, token) { deleteContact(id, token) }
                }
            })
    }

    private fun <T> handleError(response: Response<T>) {
        val errorBody = response.errorBody()?.string()
        val errorMessage = try {
            when {
                errorBody?.contains("detail") == true -> {
                    val errorResponse = Gson().fromJson(errorBody, ValidationErrorResponse::class.java)
                    errorResponse.detail?.firstOrNull()?.msg ?: "An error occurred"
                }
                errorBody?.contains("message") == true -> {
                    val errorObj = Gson().fromJson(errorBody, MessageResponse::class.java)
                    errorObj.message
                }
                else -> "An error occurred"
            }
        } catch (e: Exception) {
            "An error occurred: ${e.message}"
        }
        _error.value = errorMessage
    }

    private fun handleFailure(t: Throwable, token: String, retryAction: () -> Unit) {
        if (retryCount < maxRetries) {
            retryCount++
            retryAction.invoke()
        } else {
            retryCount = 0
            _isLoading.value = false
            _error.value = "Network error: ${t.message}"
        }
    }

    fun resetMessages() {
        _error.value = ""
        _success.value = ""
    }
}

data class ValidationErrorResponse(
    val detail: List<ValidationError>?
)

data class ValidationError(
    val loc: List<Any>?,
    val msg: String,
    val type: String
)

data class MessageResponse(
    val message: String
)
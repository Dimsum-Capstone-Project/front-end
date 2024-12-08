// HistoryViewModel.kt
package com.example.dimsumproject.ui.history_visit

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dimsumproject.data.api.ApiConfig
import com.example.dimsumproject.data.api.HistoryResponse
import com.example.dimsumproject.data.api.HistoryItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val _historyData = MutableLiveData<HistoryResponse>()
    val historyData: LiveData<HistoryResponse> = _historyData

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

    fun loadHistory() {
        _isLoading.value = true
        val client = ApiConfig.getApiService()

        client.getHistory(getAuthToken()).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(
                call: Call<HistoryResponse>,
                response: Response<HistoryResponse>
            ) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    response.body()?.let {
                        _historyData.value = it
                    }
                } else {
                    when (response.code()) {
                        401, 403 -> handleAuthError()
                        else -> _error.value = "Error: ${response.message()}"
                    }
                }
            }

            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Network error: ${t.message}"
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

    fun groupHistoryItems(items: List<HistoryItem>): Map<String, List<HistoryItem>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return items.groupBy { item ->
            val date = dateFormat.parse(item.time_scanned)
            val itemCal = Calendar.getInstance().apply { time = date!! }

            when {
                isSameDay(itemCal, today) -> "Today"
                isSameDay(itemCal, yesterday) -> "Yesterday"
                else -> "Older"
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
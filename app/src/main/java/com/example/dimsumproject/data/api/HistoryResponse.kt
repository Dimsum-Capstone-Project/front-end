// HistoryResponse.kt
package com.example.dimsumproject.data.api

data class HistoryResponse(
    val who_scanned_me: List<HistoryItem>,
    val who_i_scanned: List<HistoryItem>
)

data class HistoryItem(
    val time_scanned: String,
    val profile: Profile,
    val contacts: List<Contact>
)

data class Profile(
    val name: String,
    val bio: String,
    val job_title: String,
    val company: String,
    val profile_picture: String
)
package com.example.dimsumproject.data.api

data class RecognizePalmResponse(
    val distance: String,
    val user: ApiService.UserInfo,
    val profile: ProfileResponse
)
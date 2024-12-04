package com.example.dimsumproject.data.api

data class ProfileResponse(
    val email: String,
    val username: String,
    val bio: String?,
    val company: String?,
    val job_title: String?,
    val profile_picture: String?
)
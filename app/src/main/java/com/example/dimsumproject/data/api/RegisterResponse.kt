package com.example.dimsumproject.data.api

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("message") val message: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String
)
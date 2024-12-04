package com.example.dimsumproject.data.api

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @field:SerializedName("error")
    val error: Boolean,
    @field:SerializedName("message")
    val message: String
)
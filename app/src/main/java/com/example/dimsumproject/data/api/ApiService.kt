package com.example.dimsumproject.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/api/v1/register")
    fun registerUser(
        @Part palm_image: MultipartBody.Part,
        @Part("email") email: RequestBody,
        @Part("username") username: RequestBody,
        @Part("password") password: RequestBody
    ): Call<RegisterResponse>
}
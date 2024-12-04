package com.example.dimsumproject.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Header
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

    @Multipart
    @POST("/api/v1/profile/edit")
    fun editProfile(
        @Part("name") name: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part("job_title") jobTitle: RequestBody,
        @Part("company") company: RequestBody,
        @Part profilePicture: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Call<ProfileResponse>
}
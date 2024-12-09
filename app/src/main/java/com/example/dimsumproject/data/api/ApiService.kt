package com.example.dimsumproject.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
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

    // Login endpoint
    @POST("/api/v1/login")
    fun loginUser(
        @Body loginRequest: LoginRequest
    ): Call<LoginResponse>

    // Profile endpoint with authorization
    @GET("/api/v1/profile")
    fun getProfile(
        @Header("Authorization") token: String
    ): Call<ProfileResponse>

    // Edit profile endpoint
    @Multipart
    @POST("/api/v1/profile/edit")
    fun editProfile(
        @Part("username") username: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part("job_title") jobTitle: RequestBody,
        @Part("company") company: RequestBody,
        @Part profilePicture: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Call<ProfileResponse>

    // Contact info endpoint with authorization
    @GET("/api/v1/contact_info")
    fun getContactInfo(
        @Header("Authorization") token: String
    ): Call<ContactResponse>

    @POST("/api/v1/contact_info/add")
    fun addContactInfo(
        @Body contactInfo: ContactInfoRequest,
        @Header("Authorization") token: String
    ): Call<ContactResponse>

    @Multipart
    @POST("/api/v1/recognize_palm")
    fun recognizePalm(
        @Part palm_image: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Call<RecognizePalmResponse>

    @GET("/api/v1/history")
    fun getHistory(
        @Header("Authorization") token: String
    ): Call<HistoryResponse>


    data class LoginRequest(
        val email: String,
        val password: String
    )

    data class ContactInfoRequest(
        val contact_type: String,
        val contact_value: String,
        val notes: String
    )

    data class UserInfo(
        val email: String,
        val username: String
    )

}
package com.example.dimsumproject.data.api


import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/api/v1/register")
    fun registerUser(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("fullname") fullname: RequestBody,
        @Part("job") job: RequestBody,
        @Part("company") company: RequestBody,
        @Part("instagram") instagram: RequestBody,
        @Part("linkedin") linkedin: RequestBody,
        @Part("whatsapp") whatsapp: RequestBody,
        @Part palmImage: MultipartBody.Part,
        @Part profilePicture: MultipartBody.Part
    ): Call<RegisterResponse>
}



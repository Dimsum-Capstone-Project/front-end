package com.example.dimsumproject.ui.settings

sealed class ProfileUpdateError {
    object NetworkError : ProfileUpdateError()
    object InvalidPassword : ProfileUpdateError()
    object InvalidImage : ProfileUpdateError()
    object ServerError : ProfileUpdateError()
    data class ValidationError(val field: String, val message: String) : ProfileUpdateError()

    fun getErrorMessage(): String = when(this) {
        NetworkError -> "Koneksi internet bermasalah"
        InvalidPassword -> "Kata sandi tidak boleh sama dengan sebelumnya"
        InvalidImage -> "Format gambar tidak didukung"
        ServerError -> "Terjadi kesalahan pada server"
        is ValidationError -> message
    }
}
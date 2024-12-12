package com.example.dimsumproject.data.api

data class AddContactRequest(
    val contact_type: String,
    val contact_value: String,
    val notes: String
)

data class EditContactRequest(
    val contact_id: String,
    val contact_type: String,
    val contact_value: String,
    val notes: String
)

data class DeleteContactRequest(
    val contact_id: String
)
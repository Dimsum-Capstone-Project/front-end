package com.example.dimsumproject.data.api

data class ContactResponse(
    val contacts: List<Contact>
)

data class Contact(
    val contact_id: String,
    val contact_type: String,
    val contact_value: String,
    val notes: String?
)
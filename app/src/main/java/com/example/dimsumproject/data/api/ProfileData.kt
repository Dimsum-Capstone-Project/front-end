package com.example.dimsumproject.data.api

data class ProfileData(
    val username: String,
    val bio: String,
    val company: String,
    val jobTitle: String,
    val profilePicture: String?
) {
    fun hasChanges(newData: ProfileData): Boolean {
        return username != newData.username ||
                bio != newData.bio ||
                company != newData.company ||
                jobTitle != newData.jobTitle ||
                profilePicture != newData.profilePicture
    }
}

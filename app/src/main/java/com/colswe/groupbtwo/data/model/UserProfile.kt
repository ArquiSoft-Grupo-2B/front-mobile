package com.colswe.groupbtwo.data.model

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val alias: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
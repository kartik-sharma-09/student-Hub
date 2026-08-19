package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val studentId: String = "",
    val major: String = "",
    val institution: String = "",
    val semester: String = "",
    val targetAttendance: Int = 75,
    val avatarColorHex: String = "#4F46E5",
    val createdAt: Long = System.currentTimeMillis()
)

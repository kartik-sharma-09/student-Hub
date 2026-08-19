package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val teacher: String = "",
    val room: String = "",
    val colorHex: String = "#4F46E5",
    val targetAttendancePercent: Int = 75,
    val credits: Int = 3,
    val createdAt: Long = System.currentTimeMillis()
)

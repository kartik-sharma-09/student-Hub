package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class TaskCategory {
    ASSIGNMENT,
    EXAM,
    PROJECT,
    QUIZ,
    HOMEWORK,
    OTHER
}

@Entity(
    tableName = "assignment_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"]), Index(value = ["dueDateMillis"]), Index(value = ["isCompleted"])]
)
data class AssignmentTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long = System.currentTimeMillis() + 86400000L * 2, // Default 2 days later
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.ASSIGNMENT,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AssignmentTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM assignment_tasks ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getAllTasks(): Flow<List<AssignmentTask>>

    @Query("SELECT * FROM assignment_tasks WHERE subjectId = :subjectId ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getTasksForSubject(subjectId: Long): Flow<List<AssignmentTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AssignmentTask): Long

    @Update
    suspend fun updateTask(task: AssignmentTask)

    @Delete
    suspend fun deleteTask(task: AssignmentTask)

    @Query("DELETE FROM assignment_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE assignment_tasks SET isCompleted = :isCompleted, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun setTaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("DELETE FROM assignment_tasks WHERE subjectId = :subjectId")
    suspend fun deleteTasksForSubject(subjectId: Long)
}

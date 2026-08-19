package com.example.data.repository

import com.example.data.dao.AttendanceDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TaskDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.AssignmentTask
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class SubjectAttendanceStats(
    val subject: Subject,
    val totalRecorded: Int,
    val attendedCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val effectiveTotal: Int,
    val percentage: Float,
    val isShortage: Boolean,
    val safeSkips: Int,
    val classesNeededToMeetTarget: Int
)

data class OverallAttendanceStats(
    val totalEffectiveClasses: Int,
    val totalAttended: Int,
    val totalAbsent: Int,
    val totalCancelled: Int,
    val overallPercentage: Float,
    val subjectsCount: Int,
    val shortageSubjectsCount: Int
)

class AcademicRepository(
    private val profileDao: UserProfileDao,
    private val subjectDao: SubjectDao,
    private val attendanceDao: AttendanceDao,
    private val taskDao: TaskDao
) {
    val userProfile: Flow<UserProfile?> = profileDao.getProfile()
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    val allAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendance()
    val allTasks: Flow<List<AssignmentTask>> = taskDao.getAllTasks()

    // Combined Flow providing subjects with their calculated attendance metrics
    val subjectsWithStats: Flow<List<SubjectAttendanceStats>> = combine(
        allSubjects,
        allAttendance
    ) { subjects, attendanceList ->
        val attendanceBySubject = attendanceList.groupBy { it.subjectId }
        subjects.map { subject ->
            val records = attendanceBySubject[subject.id] ?: emptyList()
            calculateSubjectStats(subject, records)
        }
    }

    // Overall attendance stats across all subjects
    val overallStats: Flow<OverallAttendanceStats> = subjectsWithStats.combine(userProfile) { subjectStats, _ ->
        var totalAttended = 0
        var totalAbsent = 0
        var totalCancelled = 0
        var shortageCount = 0

        for (stat in subjectStats) {
            totalAttended += stat.attendedCount
            totalAbsent += stat.absentCount
            totalCancelled += stat.cancelledCount
            if (stat.isShortage && stat.effectiveTotal > 0) {
                shortageCount++
            }
        }

        val totalEffective = totalAttended + totalAbsent
        val overallPct = if (totalEffective == 0) {
            100f
        } else {
            (totalAttended.toFloat() / totalEffective.toFloat()) * 100f
        }

        OverallAttendanceStats(
            totalEffectiveClasses = totalEffective,
            totalAttended = totalAttended,
            totalAbsent = totalAbsent,
            totalCancelled = totalCancelled,
            overallPercentage = overallPct,
            subjectsCount = subjectStats.size,
            shortageSubjectsCount = shortageCount
        )
    }

    private fun calculateSubjectStats(subject: Subject, records: List<AttendanceRecord>): SubjectAttendanceStats {
        val attended = records.count { it.status == AttendanceStatus.PRESENT }
        val absent = records.count { it.status == AttendanceStatus.ABSENT }
        val cancelled = records.count { it.status == AttendanceStatus.CANCELLED }
        val effectiveTotal = attended + absent

        val targetPct = subject.targetAttendancePercent.coerceIn(1, 100)
        val targetFraction = targetPct / 100f

        val pct = if (effectiveTotal == 0) {
            100f
        } else {
            (attended.toFloat() / effectiveTotal.toFloat()) * 100f
        }

        val isShortage = effectiveTotal > 0 && pct < targetPct

        var safeSkips = 0
        var classesNeeded = 0

        if (effectiveTotal == 0) {
            safeSkips = 0
            classesNeeded = 0
        } else if (pct >= targetPct) {
            // How many consecutive classes can be skipped without dropping below targetPct?
            // (attended) / (effectiveTotal + skips) >= targetFraction
            // effectiveTotal + skips <= attended / targetFraction
            // skips <= (attended / targetFraction) - effectiveTotal
            val maxSkips = floor((attended.toFloat() / targetFraction) - effectiveTotal.toFloat()).toInt()
            safeSkips = max(0, maxSkips)
            classesNeeded = 0
        } else {
            // Shortage: How many consecutive classes must be attended to reach targetPct?
            // (attended + needed) / (effectiveTotal + needed) >= targetFraction
            // attended + needed >= targetFraction * effectiveTotal + targetFraction * needed
            // needed * (1 - targetFraction) >= targetFraction * effectiveTotal - attended
            // needed >= (targetFraction * effectiveTotal - attended) / (1 - targetFraction)
            val denominator = 1f - targetFraction
            val needed = if (denominator > 0f) {
                ceil(((targetFraction * effectiveTotal) - attended) / denominator).toInt()
            } else {
                1
            }
            classesNeeded = max(1, needed)
            safeSkips = 0
        }

        return SubjectAttendanceStats(
            subject = subject,
            totalRecorded = records.size,
            attendedCount = attended,
            absentCount = absent,
            cancelledCount = cancelled,
            effectiveTotal = effectiveTotal,
            percentage = pct,
            isShortage = isShortage,
            safeSkips = safeSkips,
            classesNeededToMeetTarget = classesNeeded
        )
    }

    // Profile Operations
    suspend fun saveProfile(profile: UserProfile) {
        profileDao.insertOrUpdate(profile)
    }

    // Subject Operations
    suspend fun addSubject(subject: Subject): Long {
        return subjectDao.insertSubject(subject)
    }

    suspend fun updateSubject(subject: Subject) {
        subjectDao.updateSubject(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        subjectDao.deleteSubject(subject)
    }

    suspend fun deleteSubjectById(id: Long) {
        subjectDao.deleteSubjectById(id)
    }

    // Attendance Operations
    fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceForSubject(subjectId)
    }

    suspend fun markAttendance(
        subjectId: Long,
        status: AttendanceStatus,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = ""
    ): Long {
        return attendanceDao.insertAttendance(
            AttendanceRecord(
                subjectId = subjectId,
                status = status,
                dateMillis = dateMillis,
                note = note
            )
        )
    }

    suspend fun deleteAttendance(record: AttendanceRecord) {
        attendanceDao.deleteAttendance(record)
    }

    suspend fun deleteAttendanceById(id: Long) {
        attendanceDao.deleteAttendanceById(id)
    }

    // Task Operations
    suspend fun addTask(task: AssignmentTask): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: AssignmentTask) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: AssignmentTask) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun toggleTaskCompletion(task: AssignmentTask) {
        val newStatus = !task.isCompleted
        val completedAt = if (newStatus) System.currentTimeMillis() else null
        taskDao.setTaskCompletion(task.id, newStatus, completedAt)
    }

    // Populate Initial Sample Data if database is empty for great first-run UX
    suspend fun prepopulateIfEmpty() {
        // Handled via ViewModel on initialization if needed
    }
}

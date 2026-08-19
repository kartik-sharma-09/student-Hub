package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AssignmentTask
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Subject
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.UserProfile
import com.example.data.repository.AcademicRepository
import com.example.data.repository.OverallAttendanceStats
import com.example.data.repository.SubjectAttendanceStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    DASHBOARD,
    ATTENDANCE,
    TASKS,
    PROFILE_SUBJECTS
}

enum class TaskFilterStatus {
    ALL,
    PENDING,
    UPCOMING,
    COMPLETED
}

class StudentHubViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AcademicRepository(
        profileDao = database.userProfileDao(),
        subjectDao = database.subjectDao(),
        attendanceDao = database.attendanceDao(),
        taskDao = database.taskDao()
    )

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(MainTab.DASHBOARD)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    // Reactive Data Flows
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allSubjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subjectsWithStats: StateFlow<List<SubjectAttendanceStats>> = repository.subjectsWithStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val overallStats: StateFlow<OverallAttendanceStats> = repository.overallStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverallAttendanceStats(0, 0, 0, 0, 100f, 0, 0)
        )

    val allTasks: StateFlow<List<AssignmentTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allAttendance: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Task Filter State
    private val _taskFilterStatus = MutableStateFlow(TaskFilterStatus.ALL)
    val taskFilterStatus: StateFlow<TaskFilterStatus> = _taskFilterStatus.asStateFlow()

    private val _taskSubjectFilterId = MutableStateFlow<Long?>(null)
    val taskSubjectFilterId: StateFlow<Long?> = _taskSubjectFilterId.asStateFlow()

    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    fun setTaskFilterStatus(filter: TaskFilterStatus) {
        _taskFilterStatus.value = filter
    }

    fun setTaskSubjectFilter(subjectId: Long?) {
        _taskSubjectFilterId.value = subjectId
    }

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    // Filtered Tasks
    val filteredTasks: StateFlow<List<AssignmentTask>> = combine(
        allTasks,
        _taskFilterStatus,
        _taskSubjectFilterId,
        _taskSearchQuery
    ) { tasks, statusFilter, subjectIdFilter, query ->
        val now = System.currentTimeMillis()
        val sevenDaysFromNow = now + (7L * 24 * 60 * 60 * 1000)

        tasks.filter { task ->
            val matchesSubject = subjectIdFilter == null || task.subjectId == subjectIdFilter
            val matchesSearch = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)

            val matchesStatus = when (statusFilter) {
                TaskFilterStatus.ALL -> true
                TaskFilterStatus.PENDING -> !task.isCompleted
                TaskFilterStatus.UPCOMING -> !task.isCompleted && task.dueDateMillis in now..sevenDaysFromNow
                TaskFilterStatus.COMPLETED -> task.isCompleted
            }

            matchesSubject && matchesSearch && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dialog & UI State Holders
    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog: StateFlow<Boolean> = _showProfileDialog.asStateFlow()

    private val _subjectToEdit = MutableStateFlow<Subject?>(null)
    val subjectToEdit: StateFlow<Subject?> = _subjectToEdit.asStateFlow()
    val showSubjectDialog = MutableStateFlow(false)

    private val _taskToEdit = MutableStateFlow<AssignmentTask?>(null)
    val taskToEdit: StateFlow<AssignmentTask?> = _taskToEdit.asStateFlow()
    val showTaskDialog = MutableStateFlow(false)

    private val _activeAttendanceSubject = MutableStateFlow<SubjectAttendanceStats?>(null)
    val activeAttendanceSubject: StateFlow<SubjectAttendanceStats?> = _activeAttendanceSubject.asStateFlow()

    private val _showCustomAttendanceDialog = MutableStateFlow<Subject?>(null)
    val showCustomAttendanceDialog: StateFlow<Subject?> = _showCustomAttendanceDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    init {
        // Prepopulate with rich initial academic data if database is fresh
        viewModelScope.launch {
            val existingProfile = repository.userProfile.first()
            val existingSubjects = repository.allSubjects.first()
            if (existingProfile == null && existingSubjects.isEmpty()) {
                seedInitialData()
            }
        }
    }

    private suspend fun seedInitialData() {
        // Seed default Student Profile
        repository.saveProfile(
            UserProfile(
                id = 1,
                name = "Alex Morgan",
                studentId = "STU-2026-889",
                major = "Computer Science",
                institution = "State University of Technology",
                semester = "Semester 4 (Spring 2026)",
                targetAttendance = 75,
                avatarColorHex = "#4F46E5"
            )
        )

        // Seed core subjects
        val csId = repository.addSubject(
            Subject(
                name = "Data Structures & Algorithms",
                code = "CS201",
                teacher = "Dr. Robert Sedgewick",
                room = "Tech Hall 301",
                colorHex = "#4F46E5",
                targetAttendancePercent = 75,
                credits = 4
            )
        )

        val mathId = repository.addSubject(
            Subject(
                name = "Linear Algebra & Probability",
                code = "MATH220",
                teacher = "Prof. Katherine Johnson",
                room = "Science Bldg 104",
                colorHex = "#0284C7",
                targetAttendancePercent = 75,
                credits = 3
            )
        )

        val dbId = repository.addSubject(
            Subject(
                name = "Database Systems & SQL",
                code = "CS240",
                teacher = "Prof. Edgar Codd",
                room = "Lab 2B",
                colorHex = "#059669",
                targetAttendancePercent = 80,
                credits = 3
            )
        )

        val osId = repository.addSubject(
            Subject(
                name = "Operating Systems",
                code = "CS310",
                teacher = "Dr. Linus Tanenbaum",
                room = "Engineering Hall 202",
                colorHex = "#D97706",
                targetAttendancePercent = 75,
                credits = 4
            )
        )

        // Seed some attendance records for realistic statistics
        val dayMs = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        // CS201: 9 Present, 1 Absent (90% - Safe)
        for (i in 1..9) {
            repository.markAttendance(csId, AttendanceStatus.PRESENT, now - (i * 2 * dayMs))
        }
        repository.markAttendance(csId, AttendanceStatus.ABSENT, now - (3 * dayMs))

        // MATH220: 6 Present, 3 Absent (66.7% - Shortage)
        for (i in 1..6) {
            repository.markAttendance(mathId, AttendanceStatus.PRESENT, now - (i * 3 * dayMs))
        }
        for (i in 1..3) {
            repository.markAttendance(mathId, AttendanceStatus.ABSENT, now - (i * 2 * dayMs))
        }

        // CS240: 8 Present, 1 Cancelled
        for (i in 1..8) {
            repository.markAttendance(dbId, AttendanceStatus.PRESENT, now - (i * 2 * dayMs))
        }
        repository.markAttendance(dbId, AttendanceStatus.CANCELLED, now - (5 * dayMs), "Campus Event")

        // CS310: 7 Present, 1 Absent
        for (i in 1..7) {
            repository.markAttendance(osId, AttendanceStatus.PRESENT, now - (i * 2 * dayMs))
        }
        repository.markAttendance(osId, AttendanceStatus.ABSENT, now - (4 * dayMs))

        // Seed initial tasks / assignments
        repository.addTask(
            AssignmentTask(
                subjectId = csId,
                title = "Implement Red-Black Tree Balancing",
                description = "Complete insertion, rotation, and deletion methods with JUnit test cases.",
                dueDateMillis = now + (1 * dayMs),
                priority = TaskPriority.HIGH,
                category = TaskCategory.ASSIGNMENT,
                isCompleted = false
            )
        )

        repository.addTask(
            AssignmentTask(
                subjectId = mathId,
                title = "Eigenvectors & Matrix Decomposition Problem Set",
                description = "Exercises 4.1 to 4.8 from Chapter 4 on orthogonal projections.",
                dueDateMillis = now + (3 * dayMs),
                priority = TaskPriority.MEDIUM,
                category = TaskCategory.HOMEWORK,
                isCompleted = false
            )
        )

        repository.addTask(
            AssignmentTask(
                subjectId = dbId,
                title = "Midterm Examination on Relational Algebra",
                description = "Covers ER diagrams, Boyce-Codd Normal Form, and complex SQL joins.",
                dueDateMillis = now + (5 * dayMs),
                priority = TaskPriority.HIGH,
                category = TaskCategory.EXAM,
                isCompleted = false
            )
        )

        repository.addTask(
            AssignmentTask(
                subjectId = osId,
                title = "Process Scheduling Simulation Project",
                description = "Implement Round Robin, Priority Scheduling, and Shortest Job First in C++.",
                dueDateMillis = now + (8 * dayMs),
                priority = TaskPriority.MEDIUM,
                category = TaskCategory.PROJECT,
                isCompleted = false
            )
        )

        repository.addTask(
            AssignmentTask(
                subjectId = csId,
                title = "Quiz 1: Asymptotic Complexity & Big-O",
                description = "Review time/space complexities of sorting algorithms.",
                dueDateMillis = now - (2 * dayMs),
                priority = TaskPriority.LOW,
                category = TaskCategory.QUIZ,
                isCompleted = true,
                completedAtMillis = now - (2 * dayMs)
            )
        )
    }

    // Profile Actions
    fun openProfileDialog() {
        _showProfileDialog.value = true
    }

    fun closeProfileDialog() {
        _showProfileDialog.value = false
    }

    fun saveProfile(
        name: String,
        studentId: String,
        major: String,
        institution: String,
        semester: String,
        targetAttendance: Int,
        avatarColorHex: String
    ) {
        viewModelScope.launch {
            repository.saveProfile(
                UserProfile(
                    id = 1,
                    name = name.trim(),
                    studentId = studentId.trim(),
                    major = major.trim(),
                    institution = institution.trim(),
                    semester = semester.trim(),
                    targetAttendance = targetAttendance.coerceIn(1, 100),
                    avatarColorHex = avatarColorHex
                )
            )
            _showProfileDialog.value = false
            showSnackbar("Profile updated successfully")
        }
    }

    // Subject Actions
    fun openAddSubjectDialog() {
        _subjectToEdit.value = null
        showSubjectDialog.value = true
    }

    fun openEditSubjectDialog(subject: Subject) {
        _subjectToEdit.value = subject
        showSubjectDialog.value = true
    }

    fun closeSubjectDialog() {
        _subjectToEdit.value = null
        showSubjectDialog.value = false
    }

    fun saveSubject(
        name: String,
        code: String,
        teacher: String,
        room: String,
        colorHex: String,
        targetPercent: Int,
        credits: Int
    ) {
        viewModelScope.launch {
            val editing = _subjectToEdit.value
            if (editing != null) {
                repository.updateSubject(
                    editing.copy(
                        name = name.trim(),
                        code = code.trim(),
                        teacher = teacher.trim(),
                        room = room.trim(),
                        colorHex = colorHex,
                        targetAttendancePercent = targetPercent.coerceIn(1, 100),
                        credits = credits
                    )
                )
                showSnackbar("Subject updated: $name")
            } else {
                repository.addSubject(
                    Subject(
                        name = name.trim(),
                        code = code.trim(),
                        teacher = teacher.trim(),
                        room = room.trim(),
                        colorHex = colorHex,
                        targetAttendancePercent = targetPercent.coerceIn(1, 100),
                        credits = credits
                    )
                )
                showSnackbar("Subject added: $name")
            }
            closeSubjectDialog()
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            showSnackbar("Deleted subject '${subject.name}'")
        }
    }

    // Attendance Actions
    fun quickMarkAttendance(subjectId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.markAttendance(subjectId, status)
            val statusLabel = when (status) {
                AttendanceStatus.PRESENT -> "Marked Present (+1)"
                AttendanceStatus.ABSENT -> "Marked Absent"
                AttendanceStatus.CANCELLED -> "Marked Class Cancelled"
            }
            showSnackbar(statusLabel)
        }
    }

    fun openCustomAttendanceDialog(subject: Subject) {
        _showCustomAttendanceDialog.value = subject
    }

    fun closeCustomAttendanceDialog() {
        _showCustomAttendanceDialog.value = null
    }

    fun recordCustomAttendance(
        subjectId: Long,
        status: AttendanceStatus,
        dateMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            repository.markAttendance(subjectId, status, dateMillis, note)
            closeCustomAttendanceDialog()
            showSnackbar("Attendance record saved")
        }
    }

    fun openAttendanceHistory(subjectStats: SubjectAttendanceStats) {
        _activeAttendanceSubject.value = subjectStats
    }

    fun closeAttendanceHistory() {
        _activeAttendanceSubject.value = null
    }

    fun deleteAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            repository.deleteAttendance(record)
            showSnackbar("Attendance record removed")
        }
    }

    // Task Actions
    fun openAddTaskDialog() {
        _taskToEdit.value = null
        showTaskDialog.value = true
    }

    fun openEditTaskDialog(task: AssignmentTask) {
        _taskToEdit.value = task
        showTaskDialog.value = true
    }

    fun closeTaskDialog() {
        _taskToEdit.value = null
        showTaskDialog.value = false
    }

    fun saveTask(
        title: String,
        description: String,
        subjectId: Long,
        dueDateMillis: Long,
        priority: TaskPriority,
        category: TaskCategory
    ) {
        viewModelScope.launch {
            val editing = _taskToEdit.value
            if (editing != null) {
                repository.updateTask(
                    editing.copy(
                        title = title.trim(),
                        description = description.trim(),
                        subjectId = subjectId,
                        dueDateMillis = dueDateMillis,
                        priority = priority,
                        category = category
                    )
                )
                showSnackbar("Task updated: $title")
            } else {
                repository.addTask(
                    AssignmentTask(
                        title = title.trim(),
                        description = description.trim(),
                        subjectId = subjectId,
                        dueDateMillis = dueDateMillis,
                        priority = priority,
                        category = category
                    )
                )
                showSnackbar("Task added: $title")
            }
            closeTaskDialog()
        }
    }

    fun toggleTaskCompletion(task: AssignmentTask) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            val msg = if (!task.isCompleted) "Task completed 🎉" else "Task marked pending"
            showSnackbar(msg)
        }
    }

    fun deleteTask(task: AssignmentTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            showSnackbar("Deleted task '${task.title}'")
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.ui.components.AttendanceHistorySheet
import com.example.ui.components.CustomAttendanceDialog
import com.example.ui.components.ProfileEditDialog
import com.example.ui.components.SubjectDialog
import com.example.ui.components.TaskDialog
import com.example.ui.components.parseHexColor
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SubjectsAndProfileScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.StudentHubViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StudentHubViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()
                val subjectsWithStats by viewModel.subjectsWithStats.collectAsStateWithLifecycle()
                val overallStats by viewModel.overallStats.collectAsStateWithLifecycle()
                val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
                val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
                val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()

                val taskFilterStatus by viewModel.taskFilterStatus.collectAsStateWithLifecycle()
                val taskSubjectFilterId by viewModel.taskSubjectFilterId.collectAsStateWithLifecycle()
                val taskSearchQuery by viewModel.taskSearchQuery.collectAsStateWithLifecycle()

                val showProfileDialog by viewModel.showProfileDialog.collectAsStateWithLifecycle()
                val showSubjectDialog by viewModel.showSubjectDialog.collectAsStateWithLifecycle()
                val subjectToEdit by viewModel.subjectToEdit.collectAsStateWithLifecycle()
                val showTaskDialog by viewModel.showTaskDialog.collectAsStateWithLifecycle()
                val taskToEdit by viewModel.taskToEdit.collectAsStateWithLifecycle()
                val activeAttendanceSubject by viewModel.activeAttendanceSubject.collectAsStateWithLifecycle()
                val showCustomAttendanceDialog by viewModel.showCustomAttendanceDialog.collectAsStateWithLifecycle()

                val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackbarMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = when (currentTab) {
                                            MainTab.DASHBOARD -> "Student Hub"
                                            MainTab.ATTENDANCE -> "Attendance Tracker"
                                            MainTab.TASKS -> "Tasks & Assignments"
                                            MainTab.PROFILE_SUBJECTS -> "Subjects & Profile"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.openProfileDialog() },
                                    modifier = Modifier.testTag("appbar_profile_button")
                                ) {
                                    val avatarBg = parseHexColor(userProfile?.avatarColorHex ?: "#4F46E5")
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(avatarBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userProfile?.name?.take(1)?.uppercase() ?: "S",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.DASHBOARD,
                                onClick = { viewModel.setTab(MainTab.DASHBOARD) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "Overview"
                                    )
                                },
                                label = { Text("Overview") },
                                modifier = Modifier.testTag("tab_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.ATTENDANCE,
                                onClick = { viewModel.setTab(MainTab.ATTENDANCE) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.ATTENDANCE) Icons.Default.HowToReg else Icons.Outlined.HowToReg,
                                        contentDescription = "Attendance"
                                    )
                                },
                                label = { Text("Attendance") },
                                modifier = Modifier.testTag("tab_attendance")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.TASKS,
                                onClick = { viewModel.setTab(MainTab.TASKS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.TASKS) Icons.Default.Assignment else Icons.Outlined.Assignment,
                                        contentDescription = "Tasks"
                                    )
                                },
                                label = { Text("Tasks") },
                                modifier = Modifier.testTag("tab_tasks")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.PROFILE_SUBJECTS,
                                onClick = { viewModel.setTab(MainTab.PROFILE_SUBJECTS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.PROFILE_SUBJECTS) Icons.Default.School else Icons.Outlined.School,
                                        contentDescription = "Subjects & Profile"
                                    )
                                },
                                label = { Text("Subjects") },
                                modifier = Modifier.testTag("tab_profile_subjects")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "TabContentTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                MainTab.DASHBOARD -> DashboardScreen(
                                    userProfile = userProfile,
                                    overallStats = overallStats,
                                    subjectsWithStats = subjectsWithStats,
                                    pendingTasks = allTasks.filter { !it.isCompleted },
                                    onNavigateTab = { viewModel.setTab(it) },
                                    onOpenProfileDialog = { viewModel.openProfileDialog() },
                                    onQuickMarkAttendance = { subjId, status ->
                                        viewModel.quickMarkAttendance(subjId, status)
                                    },
                                    onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
                                    onOpenAddTask = { viewModel.openAddTaskDialog() },
                                    onOpenAddSubject = { viewModel.openAddSubjectDialog() }
                                )

                                MainTab.ATTENDANCE -> AttendanceScreen(
                                    subjectsWithStats = subjectsWithStats,
                                    overallStats = overallStats,
                                    onQuickMarkAttendance = { subjId, status ->
                                        viewModel.quickMarkAttendance(subjId, status)
                                    },
                                    onOpenCustomAttendance = { viewModel.openCustomAttendanceDialog(it) },
                                    onOpenHistorySheet = { viewModel.openAttendanceHistory(it) },
                                    onOpenAddSubject = { viewModel.openAddSubjectDialog() }
                                )

                                MainTab.TASKS -> TasksScreen(
                                    tasks = filteredTasks,
                                    subjects = allSubjects,
                                    selectedStatusFilter = taskFilterStatus,
                                    selectedSubjectFilterId = taskSubjectFilterId,
                                    searchQuery = taskSearchQuery,
                                    onStatusFilterChanged = { viewModel.setTaskFilterStatus(it) },
                                    onSubjectFilterChanged = { viewModel.setTaskSubjectFilter(it) },
                                    onSearchQueryChanged = { viewModel.setTaskSearchQuery(it) },
                                    onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
                                    onOpenAddTask = { viewModel.openAddTaskDialog() },
                                    onOpenEditTask = { viewModel.openEditTaskDialog(it) },
                                    onDeleteTask = { viewModel.deleteTask(it) }
                                )

                                MainTab.PROFILE_SUBJECTS -> SubjectsAndProfileScreen(
                                    userProfile = userProfile,
                                    subjects = allSubjects,
                                    onOpenProfileDialog = { viewModel.openProfileDialog() },
                                    onOpenAddSubject = { viewModel.openAddSubjectDialog() },
                                    onOpenEditSubject = { viewModel.openEditSubjectDialog(it) },
                                    onDeleteSubject = { viewModel.deleteSubject(it) }
                                )
                            }
                        }
                    }

                    // Dialogs
                    if (showProfileDialog) {
                        ProfileEditDialog(
                            currentProfile = userProfile,
                            onDismiss = { viewModel.closeProfileDialog() },
                            onSave = { name, studentId, major, institution, semester, targetAttendance, avatarColorHex ->
                                viewModel.saveProfile(
                                    name,
                                    studentId,
                                    major,
                                    institution,
                                    semester,
                                    targetAttendance,
                                    avatarColorHex
                                )
                            }
                        )
                    }

                    if (showSubjectDialog) {
                        SubjectDialog(
                            subjectToEdit = subjectToEdit,
                            onDismiss = { viewModel.closeSubjectDialog() },
                            onSave = { name, code, teacher, room, colorHex, targetPercent, credits ->
                                viewModel.saveSubject(name, code, teacher, room, colorHex, targetPercent, credits)
                            }
                        )
                    }

                    if (showTaskDialog) {
                        TaskDialog(
                            taskToEdit = taskToEdit,
                            subjects = allSubjects,
                            onDismiss = { viewModel.closeTaskDialog() },
                            onSave = { title, description, subjectId, dueDateMillis, priority, category ->
                                viewModel.saveTask(title, description, subjectId, dueDateMillis, priority, category)
                            }
                        )
                    }

                    if (showCustomAttendanceDialog != null) {
                        CustomAttendanceDialog(
                            subject = showCustomAttendanceDialog!!,
                            onDismiss = { viewModel.closeCustomAttendanceDialog() },
                            onRecord = { status, dateMillis, note ->
                                viewModel.recordCustomAttendance(
                                    showCustomAttendanceDialog!!.id,
                                    status,
                                    dateMillis,
                                    note
                                )
                            }
                        )
                    }

                    if (activeAttendanceSubject != null) {
                        val recordsForSubject = allAttendance.filter { it.subjectId == activeAttendanceSubject!!.subject.id }
                        AttendanceHistorySheet(
                            subjectStats = activeAttendanceSubject!!,
                            records = recordsForSubject,
                            onDismiss = { viewModel.closeAttendanceHistory() },
                            onDeleteRecord = { viewModel.deleteAttendanceRecord(it) },
                            onOpenCustomAttendance = {
                                val subj = activeAttendanceSubject!!.subject
                                viewModel.closeAttendanceHistory()
                                viewModel.openCustomAttendanceDialog(subj)
                            }
                        )
                    }
                }
            }
        }
    }
}

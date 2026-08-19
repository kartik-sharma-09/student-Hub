package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.UserProfile
import com.example.data.repository.OverallAttendanceStats
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(
          userProfile = UserProfile(name = "Alex Morgan", studentId = "STU-2026", major = "Computer Science"),
          overallStats = OverallAttendanceStats(10, 8, 2, 0, 80f, 4, 0),
          subjectsWithStats = emptyList(),
          pendingTasks = emptyList(),
          onNavigateTab = {},
          onOpenProfileDialog = {},
          onQuickMarkAttendance = { _, _ -> },
          onToggleTaskCompletion = {},
          onOpenAddTask = {},
          onOpenAddSubject = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

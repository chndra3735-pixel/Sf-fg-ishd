package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.SwarRoomTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SwarRoom", appName)
  }

  @Test
  fun `main activity launches without crashing`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertEquals("SwarRoom", activity.getString(R.string.app_name))
  }

  @Test
  fun `root composable shows setup screen instead of crashing when firebase is uninitialized`() {
    var attempts = 0
    composeTestRule.setContent {
      SwarRoomTheme {
        SwarRoomAppRoot(
          firebaseAuthProvider = {
            attempts++
            null
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("retry_firebase_init_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("retry_firebase_init_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(2, attempts)
  }
}

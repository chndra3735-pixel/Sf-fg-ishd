package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.MediaCompressor
import com.example.ui.theme.SwarRoomTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

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

  @Test
  fun `media compressor prepares jpg and transparent png gift photos`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val sampleJpg = File(context.cacheDir, "sample_gift_dp.jpg")
      val samplePng = File(context.cacheDir, "sample_gift_dp.png")
      val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
      bmp.eraseColor(android.graphics.Color.argb(180, 255, 80, 120))
      try {
        sampleJpg.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        samplePng.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val preparedJpg = MediaCompressor.prepareForUpload(
          context = context,
          uri = Uri.fromFile(sampleJpg),
          folder = "gifts",
          isVideo = false,
        )
        assertTrue(preparedJpg.file.exists() && preparedJpg.finalBytes > 0L)
        assertEquals("image/jpeg", preparedJpg.mimeType)
        preparedJpg.file.delete()

        val preparedPng = MediaCompressor.prepareForUpload(
          context = context,
          uri = Uri.fromFile(samplePng),
          folder = "gifts",
          isVideo = false,
        )
        assertTrue(preparedPng.file.exists() && preparedPng.finalBytes > 0L)
        assertTrue(preparedPng.mimeType == "image/webp" || preparedPng.mimeType == "image/jpeg")
        preparedPng.file.delete()
      } finally {
        bmp.recycle()
        sampleJpg.delete()
        samplePng.delete()
      }
    }
  }
}

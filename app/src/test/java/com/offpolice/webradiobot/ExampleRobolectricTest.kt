package com.offpolice.webradiobot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.offpolice.webradiobot.player.RadioPlayerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WebRadioBot", appName)
  }

  @Test
  fun `player manager instance is available`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = RadioPlayerManager.getInstance(context)
    assertNotNull(manager)
  }

  @Test
  fun `initStationWithoutPlayback initializes metadata without starting player`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = RadioPlayerManager.getInstance(context)
    manager.initStationWithoutPlayback("https://stream.example.com/test", "Test Station", null)
    assertEquals("https://stream.example.com/test", manager.currentUrl.value)
    assertEquals("Test Station", manager.currentName.value)
  }

  @Test
  fun `playNextStation invokes registered callback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = RadioPlayerManager.getInstance(context)
    var nextInvoked = false
    manager.onNextStation = { nextInvoked = true }
    manager.playNextStation()
    assertEquals(true, nextInvoked)
  }
}

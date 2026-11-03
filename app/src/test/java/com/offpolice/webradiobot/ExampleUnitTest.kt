package com.offpolice.webradiobot

import com.offpolice.webradiobot.updater.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `version comparison recognizes newer semantic version`() {
    assertTrue(AppUpdateManager.isNewerVersion("1.0.1", "1.0.0"))
    assertTrue(AppUpdateManager.isNewerVersion("v1.0.1", "1.0.0"))
    assertTrue(AppUpdateManager.isNewerVersion("v1.1.0", "v1.0.0"))
    assertTrue(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
    assertTrue(AppUpdateManager.isNewerVersion("1.0.0.1", "1.0.0"))
    assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
    assertFalse(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.0"))
    assertFalse(AppUpdateManager.isNewerVersion("0.9.9", "1.0.0"))
    assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
  }
}

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
  fun testCleanVersion() {
    assertEquals("1.0.0", AppUpdateManager.cleanVersion("v1.0.0"))
    assertEquals("1.0.0", AppUpdateManager.cleanVersion("V1.0.0"))
    assertEquals("1.0.0", AppUpdateManager.cleanVersion(" 1.0.0 "))
  }

  @Test
  fun testIsNewerVersion() {
    assertTrue(AppUpdateManager.isNewerVersion("v1.0.1", "1.0.0"))
    assertTrue(AppUpdateManager.isNewerVersion("v2.0.0", "1.9.9"))
    assertTrue(AppUpdateManager.isNewerVersion("1.1", "1.0.9"))
    assertTrue(AppUpdateManager.isNewerVersion("v1.0.0.1", "1.0.0"))

    assertFalse(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.0"))
    assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
    assertFalse(AppUpdateManager.isNewerVersion("v0.9.9", "1.0.0"))
  }
}

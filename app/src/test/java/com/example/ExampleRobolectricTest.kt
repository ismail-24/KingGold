package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("King Gold", appName)
  }

  @Test
  fun `configureOsmdroid sets user agent and app internal tile directories`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.ui.components.configureOsmdroid(context)
    val config = org.osmdroid.config.Configuration.getInstance()
    assertEquals(context.packageName.ifBlank { "com.example.kinggold" }, config.userAgentValue)
    assertEquals(true, config.osmdroidBasePath.absolutePath.contains("osmdroid"))
    assertEquals(true, config.osmdroidTileCache.absolutePath.contains("osmdroid"))
  }
}

package com.android.mySwissDorm.ui

import android.content.Context
import androidx.test.core.app.ActivityScenario
import com.android.mySwissDorm.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityLocaleTest {

  @Test
  fun updateLanguage_savesPreference() {
    MainActivity.enableStreamInitialization = false

    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.onActivity { activity ->
      activity.updateLanguage("fr")

      val prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
      assertEquals("fr", prefs.getString("app_language", null))
    }
  }
}

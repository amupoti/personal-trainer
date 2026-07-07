package com.marcel.personaltrainer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceTest {
    @Test
    fun `stored values restore each preference`() {
        ThemePreference.entries.forEach { preference ->
            assertEquals(preference, ThemePreference.fromStoredValue(preference.name))
        }
    }

    @Test
    fun `missing or invalid stored value uses system theme`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStoredValue(null))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStoredValue("INVALID"))
    }
}

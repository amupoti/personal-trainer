package com.marcel.personaltrainer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {
    @Test
    fun detectsNewerMajorMinorAndPatchVersions() {
        assertTrue(isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(isNewerVersion("1.1.0", "1.0.9"))
        assertTrue(isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun rejectsEqualAndOlderVersions() {
        assertFalse(isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(isNewerVersion("1.0", "1.0.0"))
        assertFalse(isNewerVersion("1.9.9", "2.0.0"))
    }

    @Test
    fun rejectsMalformedVersions() {
        assertFalse(isNewerVersion("latest", "1.0.0"))
        assertFalse(isNewerVersion("1.0.0", "unknown"))
    }
}

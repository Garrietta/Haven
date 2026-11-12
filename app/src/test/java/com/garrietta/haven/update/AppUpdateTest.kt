package com.garrietta.haven.update

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    private val release = JSONObject(
        """
        {"name": "Test build 152", "html_url": "https://github.com/Garrietta/Haven/releases/tag/test-build",
         "published_at": "2026-11-12T18:00:00Z",
         "body": "Build: 152\nBranch: claude/x\nCommit: abc1234\n\nThe newest signed build.",
         "assets": [{"name": "haven-build-152.apk", "browser_download_url": "https://example.invalid/haven.apk", "size": 31457280}]}
        """,
    )

    @Test
    fun aTestBuildReadsWithItsNumberAndApk() {
        val update = parseTestBuild(release)!!
        assertEquals(152, update.build)
        assertEquals("https://example.invalid/haven.apk", update.apkUrl)
        assertEquals(31457280L, update.apkSize)
        // The machine line is left out of what the page shows.
        assertFalse(update.notes.contains("Build:"))
        assertTrue(update.notes.contains("Commit: abc1234"))
    }

    @Test
    fun aHigherBuildIsNewer() {
        val update = parseTestBuild(release)!!
        assertTrue(isNewer(update, 151))
        assertFalse(isNewer(update, 152))
        assertFalse(isNewer(update, 160))
        // A local build (0) is offered the newest test build.
        assertTrue(isNewer(update, 0))
    }

    @Test
    fun aReleaseWithoutANumberOrAnApkIsNotAnUpdate() {
        assertNull(parseTestBuild(JSONObject("""{"name": "2.0", "body": "Notes", "assets": [{"name": "x.apk"}]}""")))
        assertNull(parseTestBuild(JSONObject("""{"name": "Test build 3", "body": "", "assets": []}""")))
        assertEquals(176, testBuildNumber("", "Test build 176"))
    }
}

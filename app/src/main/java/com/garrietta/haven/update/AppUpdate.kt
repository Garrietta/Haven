package com.garrietta.haven.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/*
 * Where Haven's updates come from. CI replaces one pre-release, tagged
 * test-build, after every signed build: it carries the APK and a
 * "Build: <run number>" line in its notes. This app knows its own run
 * number (BuildConfig.BUILD_NUMBER), so a higher number is newer. The
 * repository is public, so no sign-in is needed to read it.
 */

const val TEST_BUILD_TAG = "test-build"

/** The newest test build, as the release describes it. */
data class AppUpdate(
    val build: Int,
    val title: String,
    /** What the build says about itself, without its "Build:" line. */
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String,
    val publishedAt: String,
)

sealed class UpdateCheck {
    data class Available(val update: AppUpdate) : UpdateCheck()
    data class Current(val update: AppUpdate) : UpdateCheck()
    data object NoBuilds : UpdateCheck()
    data class Failed(val message: String) : UpdateCheck()
}

/** The build number in a test-build release: "Build: 175" in its notes, or "Test build 175" as its name. */
fun testBuildNumber(notes: String, name: String): Int? =
    Regex("""(?im)^\s*Build:\s*(\d+)\s*$""").find(notes)?.groupValues?.get(1)?.toIntOrNull()
        ?: Regex("""(?i)test build\s+(\d+)""").find(name)?.groupValues?.get(1)?.toIntOrNull()

/** Reads a GitHub release into an [AppUpdate]; null when it has no build number or no APK. */
fun parseTestBuild(release: JSONObject): AppUpdate? {
    val name = release.optString("name")
    val body = release.optString("body")
    val build = testBuildNumber(body, name) ?: return null
    val assets = release.optJSONArray("assets") ?: return null
    val apk = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
        .firstOrNull { it.optString("name").endsWith(".apk") } ?: return null
    return AppUpdate(
        build = build,
        title = name.ifEmpty { "Test build $build" },
        notes = body.lines().filterNot { it.trim().startsWith("Build:", ignoreCase = true) }.joinToString("\n").trim(),
        apkUrl = apk.optString("browser_download_url"),
        apkSize = apk.optLong("size", 0L),
        pageUrl = release.optString("html_url"),
        publishedAt = release.optString("published_at"),
    )
}

/** Whether [update] is newer than the build installed. A local build (0) is never offered its own number. */
fun isNewer(update: AppUpdate, installedBuild: Int): Boolean = update.build > installedBuild

private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

/** Asks GitHub for the newest test build of [repo] and compares it with [installedBuild]. */
suspend fun checkForUpdate(repo: String, installedBuild: Int): UpdateCheck = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/tags/$TEST_BUILD_TAG")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Haven-Android")
            .build()
        client.newCall(request).execute().use { response ->
            when (response.code) {
                200 -> {
                    val update = response.body?.string()?.let { parseTestBuild(JSONObject(it)) }
                        ?: return@use UpdateCheck.Failed("The test build does not say which build it is.")
                    if (isNewer(update, installedBuild)) UpdateCheck.Available(update) else UpdateCheck.Current(update)
                }
                404 -> UpdateCheck.NoBuilds
                else -> UpdateCheck.Failed("GitHub answered ${response.code}.")
            }
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        UpdateCheck.Failed(e.message ?: "Could not reach GitHub.")
    }
}

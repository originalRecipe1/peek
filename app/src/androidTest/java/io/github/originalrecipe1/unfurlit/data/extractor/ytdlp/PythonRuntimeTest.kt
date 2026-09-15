package io.github.originalrecipe1.unfurlit.data.extractor.ytdlp

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.github.originalrecipe1.unfurlit.BuildConfig
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PythonRuntimeTest {
    @Test(timeout = 120_000)
    fun runtimeUpgradeRemovesObsoleteFilesAndPreservesDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BundledYtDlpInstaller(context).ensureCurrent()
        val engine = YoutubeDL.getInstance()
        engine.init(context)
        val runtime = File(context.noBackupFilesDir, "youtubedl-android/packages/python")
        val obsolete = File(runtime, "usr/lib/quickjs/libquickjs.a")
        val databaseName = "runtime-upgrade-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
                database.execSQL("CREATE TABLE history (title TEXT NOT NULL)")
                database.execSQL("INSERT INTO history VALUES ('Saved visit')")
                obsolete.parentFile!!.mkdirs()
                obsolete.writeText("obsolete runtime build file")
                // Simulate the archive-size marker stored by the previous runtime.
                context.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
                    .edit().putString("pythonLibVersion", "previous-archive-size").apply()

                engine.initPython(context, runtime)

                assertFalse(obsolete.exists())
                database.rawQuery("SELECT title FROM history", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Saved visit", cursor.getString(0))
                }
                val request = YoutubeDLRequest(emptyList()).apply { addOption("--version") }
                assertEquals(BuildConfig.YT_DLP_ENGINE_VERSION, engine.execute(request).out.trim())
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test(timeout = 120_000)
    fun trimmedRuntimeStartsThePinnedEngineWithoutNetworkAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BundledYtDlpInstaller(context).ensureCurrent()
        val engine = YoutubeDL.getInstance()
        engine.init(context)
        val request = YoutubeDLRequest(emptyList()).apply { addOption("--version") }
        assertEquals(BuildConfig.YT_DLP_ENGINE_VERSION, engine.execute(request).out.trim())

        val runtime = File(context.noBackupFilesDir, "youtubedl-android/packages/python")
        assertFalse(File(runtime, "usr/lib/quickjs/libquickjs.a").exists())
        val modules = File(runtime, "usr/lib/python3.12/lib-dynload").listFiles().orEmpty()
        assertFalse(modules.any { it.name.startsWith("_test") && it.extension == "so" })
    }
}

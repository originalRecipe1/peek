package org.peek.app.data.extractor.ytdlp

import android.content.Context
import android.system.Os
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import org.peek.app.BuildConfig
import org.peek.app.R
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Keeps youtubedl-android's private extractor copy in sync with the version
 * bundled in this APK. The upstream library only copies the resource when its
 * destination does not exist, which otherwise leaves upgrades on an old engine.
 */
internal class BundledYtDlpInstaller(
    private val appContext: Context,
) {
    @Throws(YoutubeDLException::class)
    fun ensureCurrent() {
        val extractorDirectory = File(
            File(appContext.noBackupFilesDir, YoutubeDL.baseName),
            YoutubeDL.ytdlpDirName,
        )
        val extractorFile = File(extractorDirectory, YoutubeDL.ytdlpBin)
        val installedHash = runCatching { extractorFile.sha256() }.getOrNull()

        if (!bundledYtDlpNeedsRefresh(installedHash, BuildConfig.YT_DLP_ENGINE_SHA256)) {
            return
        }

        try {
            if (!extractorDirectory.exists() && !extractorDirectory.mkdirs()) {
                error("Could not create the yt-dlp directory")
            }

            val temporaryFile = File.createTempFile("ytdlp-", ".part", extractorDirectory)
            try {
                val bundledHash = copyBundledExtractor(temporaryFile)
                check(
                    bundledHash.equals(BuildConfig.YT_DLP_ENGINE_SHA256, ignoreCase = true),
                ) {
                    "Bundled yt-dlp checksum does not match the build configuration"
                }

                // POSIX rename replaces the old file atomically because both files
                // live in the same app-private directory.
                Os.rename(temporaryFile.absolutePath, extractorFile.absolutePath)
                Log.i(TAG, "Activated bundled yt-dlp ${BuildConfig.YT_DLP_ENGINE_VERSION}")
            } finally {
                temporaryFile.delete()
            }
        } catch (error: Exception) {
            throw YoutubeDLException("Failed to activate the bundled yt-dlp engine", error)
        }
    }

    private fun copyBundledExtractor(destination: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        appContext.resources.openRawResource(R.raw.ytdlp).use { input ->
            FileOutputStream(destination).use { fileOutput ->
                val output = BufferedOutputStream(fileOutput)
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
                output.flush()
                fileOutput.fd.sync()
            }
        }
        return digest.digest().toHexString()
    }

    private fun File.sha256(): String? {
        if (!isFile) return null
        return FileInputStream(this).buffered().use(::sha256)
    }

    private fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest().toHexString()
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "BundledYtDlpInstaller"
        const val COPY_BUFFER_SIZE = 64 * 1024
    }
}

internal fun bundledYtDlpNeedsRefresh(
    installedSha256: String?,
    bundledSha256: String,
): Boolean = !installedSha256.equals(bundledSha256, ignoreCase = true)

package org.peek.app.data.extractor.ytdlp

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.peek.app.domain.extractor.MediaExtractor
import org.peek.app.domain.model.ExtractionError
import org.peek.app.domain.model.ExtractionException
import org.peek.app.domain.model.ExtractionResult
import org.peek.app.util.SafeLog
import org.peek.app.util.UrlValidator
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.coroutines.resumeWithException

class YtDlpMediaExtractor(
    context: Context,
) : MediaExtractor {
    private val appContext = context.applicationContext
    private val bundledYtDlpInstaller = BundledYtDlpInstaller(appContext)

    override suspend fun extract(url: String): ExtractionResult {
        if (!UrlValidator.isAllowed(url)) {
            throw ExtractionException(ExtractionError.UnsupportedUrl)
        }

        val processId = "peek-${UUID.randomUUID()}"
        val request = YoutubeDLRequest(url).apply {
            addOption("--ignore-config")
            addOption("--dump-single-json")
            addOption("--skip-download")
            addOption("--playlist-end", MAX_MEDIA_ENTRIES.toString())
            addOption("--no-warnings")
            addOption("--format", FORMAT_SELECTOR)
        }

        return try {
            val output = executeCancellable(request, processId)
            YtDlpJsonParser.parse(url, output).also { result ->
                Log.d(
                    TAG,
                    "Extracted platform=${result.platform}, mediaCount=${result.media.size}",
                )
            }
        } catch (error: ExtractionException) {
            logFailure(error)
            throw error
        } catch (error: Throwable) {
            logFailure(error)
            throw ExtractionException(error.toDomainError(), error)
        }
    }

    private suspend fun executeCancellable(
        request: YoutubeDLRequest,
        processId: String,
    ): String = suspendCancellableCoroutine { continuation ->
        val future = executor.submit {
            try {
                ensureInitialized()
                val output = YoutubeDL.getInstance().execute(request, processId).out
                continuation.resumeWith(Result.success(output))
            } catch (error: Throwable) {
                continuation.resumeWithException(error)
            }
        }

        continuation.invokeOnCancellation {
            future.cancel(true)
            runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
        }
    }

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(initializationLock) {
            if (!initialized) {
                bundledYtDlpInstaller.ensureCurrent()
                YoutubeDL.getInstance().init(appContext)
                initialized = true
            }
        }
    }

    private fun Throwable.toDomainError(): ExtractionError {
        val detail = generateSequence(this) { it.cause }
            .mapNotNull(Throwable::message)
            .joinToString(" ")
            .lowercase()

        return when {
            this is IOException || listOf("network", "timed out", "connection", "dns").any(detail::contains) ->
                ExtractionError.NetworkFailure
            listOf("sign in", "login", "cookies", "private video", "authentication").any(detail::contains) ->
                ExtractionError.AuthenticationRequired
            listOf("unsupported url", "no suitable extractor").any(detail::contains) ->
                ExtractionError.UnsupportedUrl
            listOf("unavailable", "removed", "deleted", "not available").any(detail::contains) ->
                ExtractionError.MediaUnavailable
            this is YoutubeDLException -> ExtractionError.ExtractionFailed
            else -> ExtractionError.ExtractionFailed
        }
    }

    private fun logFailure(error: Throwable) {
        val detail = generateSequence(error) { it.cause }
            .mapNotNull(Throwable::message)
            .joinToString(" | ")
        Log.e(
            TAG,
            "Extraction failed (${error::class.java.simpleName}): ${SafeLog.redact(detail)}",
        )
    }

    private companion object {
        const val TAG = "YtDlpExtractor"
        const val FORMAT_SELECTOR =
            "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best"
        const val MAX_MEDIA_ENTRIES = 50
        val executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "peek-ytdlp").apply { isDaemon = true }
        }
        val initializationLock = Any()

        @Volatile
        var initialized = false
    }
}

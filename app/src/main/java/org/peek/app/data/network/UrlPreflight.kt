package org.peek.app.data.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.peek.app.util.UrlValidator
import java.io.IOException
import kotlin.coroutines.resumeWithException

class UrlPreflight(
    private val client: OkHttpClient = SafeHttpClient.preflight,
) {
    suspend fun resolve(url: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", USER_AGENT)
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val resolvedUrl = response.request.url.toString()
                    if (!UrlValidator.isAllowed(resolvedUrl)) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                UnsafeNetworkTargetException(),
                            )
                        }
                    } else if (continuation.isActive) {
                        continuation.resumeWith(Result.success(resolvedUrl))
                    }
                }
            }
        })
    }

    private companion object {
        const val USER_AGENT = "Peek URL safety preflight"
    }
}

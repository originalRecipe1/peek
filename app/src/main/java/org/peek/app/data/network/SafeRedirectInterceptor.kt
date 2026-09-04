package org.peek.app.data.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.peek.app.util.UrlValidator
import java.net.ProtocolException

class SafeRedirectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            requirePublicUrl(request.url)
            val response = chain.proceed(request)
            val location = response.header("Location")
            if (!response.isRedirect || location == null) return response

            val target = response.request.url.resolve(location) ?: return response
            response.close()
            if (redirectCount == MAX_REDIRECTS) {
                throw ProtocolException("Too many redirects")
            }
            requirePublicUrl(target)
            request = request.redirectedTo(target)
        }
        error("Unreachable")
    }

    private fun requirePublicUrl(url: HttpUrl) {
        if (!UrlValidator.isAllowedHttps(url.toString())) {
            throw UnsafeNetworkTargetException()
        }
    }

    private fun Request.redirectedTo(target: HttpUrl): Request =
        newBuilder()
            .url(target)
            .apply {
                if (!url.isSameOrigin(target)) {
                    removeHeader("Authorization")
                    removeHeader("Cookie")
                    removeHeader("Host")
                    removeHeader("Proxy-Authorization")
                }
            }
            .build()

    private fun HttpUrl.isSameOrigin(other: HttpUrl): Boolean =
        scheme == other.scheme && host == other.host && port == other.port

    private companion object {
        const val MAX_REDIRECTS = 20
    }
}

package org.peek.app.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SafeHttpClient {
    val streaming: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(PublicOnlyDns())
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(SafeRedirectInterceptor())
            .build()
    }

    val preflight: OkHttpClient by lazy {
        streaming.newBuilder()
            .callTimeout(PREFLIGHT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private const val PREFLIGHT_TIMEOUT_SECONDS = 20L
}

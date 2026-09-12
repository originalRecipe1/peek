package io.github.originalrecipe1.unfurlit.data.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import io.github.originalrecipe1.unfurlit.domain.model.PlaybackCookie

/** One media source's cookies, matched anew for every request and redirect. */
internal class PlaybackCookieJar(cookies: List<PlaybackCookie>) : CookieJar {
    private val scopedCookies = cookies.map { cookie ->
        Cookie.Builder().name(cookie.name).value(cookie.value).path(cookie.path)
            .expiresAt(cookie.expiresAtMillis)
            .apply {
                if (cookie.hostOnly) hostOnlyDomain(cookie.domain) else domain(cookie.domain)
                if (cookie.secure) secure()
            }.build()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = scopedCookies.filter {
        it.expiresAt > System.currentTimeMillis() && it.matches(url)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = Unit
}

package org.peek.app.util

import java.net.InetAddress
import java.net.URI

object UrlValidator {
    fun isAllowed(rawUrl: String): Boolean {
        if (rawUrl.length !in 1..MAX_URL_LENGTH) return false
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return false

        val host = uri.host?.trimEnd('.')?.lowercase() ?: return false
        if (host == "localhost" || host.endsWith(".localhost")) return false

        val literalAddress = parseIpLiteral(host) ?: return true
        return !literalAddress.isAnyLocalAddress &&
            !literalAddress.isLoopbackAddress &&
            !literalAddress.isLinkLocalAddress &&
            !literalAddress.isSiteLocalAddress &&
            !literalAddress.isMulticastAddress
    }

    fun toHttpsUrl(rawUrl: String): String? {
        if (!isAllowed(rawUrl)) return null
        val scheme = runCatching { URI(rawUrl).scheme }.getOrNull() ?: return null
        return when (scheme.lowercase()) {
            "https" -> rawUrl
            "http" -> "https${rawUrl.substring(scheme.length)}"
            else -> null
        }
    }

    fun isAllowedHttps(rawUrl: String): Boolean =
        isAllowed(rawUrl) &&
            runCatching { URI(rawUrl).scheme.equals("https", ignoreCase = true) }
                .getOrDefault(false)

    private fun parseIpLiteral(host: String): InetAddress? {
        val normalized = host.removePrefix("[").removeSuffix("]")
        val looksLikeIpv4 = normalized.matches(Regex("[0-9.]+"))
        val looksLikeIpv6 = ':' in normalized
        if (!looksLikeIpv4 && !looksLikeIpv6) return null
        return runCatching { InetAddress.getByName(normalized) }.getOrNull()
    }

    private const val MAX_URL_LENGTH = 8 * 1024
}

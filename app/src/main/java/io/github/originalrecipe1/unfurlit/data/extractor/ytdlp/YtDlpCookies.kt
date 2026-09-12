package io.github.originalrecipe1.unfurlit.data.extractor.ytdlp

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import io.github.originalrecipe1.unfurlit.domain.model.PlaybackCookie

/** Parses yt-dlp's scoped cookie serialization, not an unscoped Cookie header. */
internal object YtDlpCookies {
    fun parse(serialized: String?, sourceUrl: String, now: Long = System.currentTimeMillis()): List<PlaybackCookie> {
        if (serialized.isNullOrEmpty() || serialized.length > 32_768) return emptyList()
        val url = sourceUrl.toHttpUrlOrNull() ?: return emptyList()
        val tokens = tokenize(serialized) ?: return emptyList()
        val groups = mutableListOf<MutableList<String>>()
        for (token in tokens) {
            val name = token.substringBefore('=').trim().lowercase()
            if (name in setOf("domain", "path", "secure", "expires", "version", "httponly", "samesite")) {
                groups.lastOrNull()?.add(token) ?: return emptyList()
            } else {
                if ('=' !in token || groups.size >= 64) return emptyList()
                groups.add(mutableListOf(token))
            }
        }
        return groups.mapNotNull { group ->
            val attributes = group.drop(1).associate {
                it.substringBefore('=').trim().lowercase() to it.substringAfter('=', "").trim()
            }
            // yt-dlp always supplies scope for exported cookies; never guess it.
            if (attributes["domain"].isNullOrBlank()) return@mapNotNull null
            val expires = attributes["expires"]?.let {
                val seconds = it.toLongOrNull() ?: return@mapNotNull null
                if (seconds <= 0 || seconds > Long.MAX_VALUE / 1000) return@mapNotNull null
                seconds * 1000
            } ?: Long.MAX_VALUE
            if (expires <= now) return@mapNotNull null
            // Let OkHttp validate name/value, domain and path. Its HTTP-date parser
            // cannot read yt-dlp's epoch-seconds Expires, so apply that separately.
            val decodedValue = decodeValue(group.first().substringAfter('=').trim()) ?: return@mapNotNull null
            val pair = group.first().substringBefore('=').trim() + "=" + decodedValue
            val header = (listOf(pair) + group.drop(1)).filterNot { it.substringBefore('=').trim().equals("expires", true) }.joinToString("; ")
            val parsed = Cookie.parse(url, header) ?: return@mapNotNull null
            if (!parsed.matches(url)) return@mapNotNull null
            PlaybackCookie(parsed.name, parsed.value, parsed.domain, parsed.path, expires, parsed.secure, parsed.hostOnly)
        }
    }

    // Python's cookie serializer quotes/escapes values. Sending those serialization
    // quotes literally causes TikTok to reject tt_chain_token with HTTP 403.
    private fun decodeValue(encoded: String): String? {
        val value = if (encoded.startsWith('"')) {
            if (!encoded.endsWith('"') || encoded.length < 2) return null
            val inner = encoded.substring(1, encoded.length - 1)
            val result = StringBuilder()
            var index = 0
            while (index < inner.length) {
                if (inner[index] != '\\') {
                    result.append(inner[index++])
                } else {
                    index++
                    if (index >= inner.length) return null
                    val octal = inner.substring(index, minOf(index + 3, inner.length))
                    if (octal.length == 3 && octal[0] in '0'..'3' && octal.all { it in '0'..'7' }) {
                        result.append(octal.toInt(8).toChar())
                        index += 3
                    } else {
                        result.append(inner[index++])
                    }
                }
            }
            result.toString()
        } else encoded
        // Fail closed rather than turn a decoded delimiter into another cookie.
        return value.takeIf { it.all { ch -> ch.code in 33..126 && ch !in "\",;\\" } }
    }

    private fun tokenize(value: String): List<String>? {
        val tokens = mutableListOf<String>()
        var quoted = false
        var escaped = false
        var start = 0
        for ((index, character) in value.withIndex()) {
            if (character.code < 32 || character.code == 127) return null
            if (escaped) { escaped = false; continue }
            when (character) {
                '\\' -> if (quoted) escaped = true
                '"' -> quoted = !quoted
                ';' -> if (!quoted) { tokens.add(value.substring(start, index).trim()); start = index + 1 }
            }
        }
        if (quoted || escaped) return null
        tokens.add(value.substring(start).trim())
        return tokens.filter(String::isNotEmpty)
    }
}

package org.peek.app.util

object UrlTextParser {
    fun firstSupportedUrl(text: CharSequence?): String? {
        if (text.isNullOrBlank()) return null
        val boundedText = text.take(MAX_SHARED_TEXT_LENGTH).trim().toString()
        if (UrlValidator.isAllowed(boundedText)) return boundedText

        return HTTP_URL.findAll(boundedText)
            .map { match -> match.value.trimEnd(*TRAILING_PROSE) }
            .firstOrNull(UrlValidator::isAllowed)
    }

    private const val MAX_SHARED_TEXT_LENGTH = 64 * 1024
    private val HTTP_URL = Regex("https?://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE)
    private val TRAILING_PROSE = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}')
}

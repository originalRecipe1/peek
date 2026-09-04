package org.peek.app.util

object SafeLog {
    fun redact(message: String?): String {
        if (message.isNullOrBlank()) return "No detail"
        return message
            .replace(URL, "<url>")
            .replace(SECRET, "$1=<redacted>")
            .replace(WHITESPACE, " ")
            .take(MAX_LENGTH)
    }

    private const val MAX_LENGTH = 1_000
    private val URL = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
    private val SECRET = Regex(
        "(?i)\\b(cookie|authorization|token|password)\\b\\s*[:=]\\s*\\S+",
    )
    private val WHITESPACE = Regex("\\s+")
}

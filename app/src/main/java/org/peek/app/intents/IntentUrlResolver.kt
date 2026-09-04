package org.peek.app.intents

import android.content.Intent
import org.peek.app.util.UrlTextParser

object IntentUrlResolver {
    fun resolve(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_VIEW -> UrlTextParser.firstSupportedUrl(intent.dataString)
        Intent.ACTION_SEND -> if (intent.type == MIME_TEXT_PLAIN) {
            val sharedText = runCatching {
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            }.getOrNull()
            UrlTextParser.firstSupportedUrl(sharedText)
        } else {
            null
        }

        else -> null
    }

    private const val MIME_TEXT_PLAIN = "text/plain"
}

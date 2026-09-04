package org.peek.app.data.history

import org.peek.app.domain.model.ExtractedMedia
import org.peek.app.domain.model.ExtractionResult
import org.peek.app.domain.model.HistoryEntry
import org.peek.app.domain.model.HistoryMediaKind

internal object HistoryEntryMapper {
    fun fromExtraction(
        result: ExtractionResult,
        viewedAtEpochMillis: Long,
    ): HistoryEntry = HistoryEntry(
        id = 0,
        sourceUrl = result.sourceUrl,
        platform = result.platform.cleaned(MAX_PLATFORM_LENGTH),
        title = result.title.cleaned(MAX_TITLE_LENGTH),
        author = result.author.cleaned(MAX_AUTHOR_LENGTH),
        mediaKind = result.media.toHistoryKind(),
        mediaCount = result.media.size,
        durationSeconds = result.media.firstNotNullOfOrNull { media ->
            when (media) {
                is ExtractedMedia.Video -> media.durationSeconds
                is ExtractedMedia.Audio -> media.durationSeconds
                is ExtractedMedia.Image -> null
            }
        },
        viewedAtEpochMillis = viewedAtEpochMillis,
    )

    private fun List<ExtractedMedia>.toHistoryKind(): HistoryMediaKind {
        if (size > 1 && all { it is ExtractedMedia.Image }) return HistoryMediaKind.Gallery
        val kinds = map { media ->
            when (media) {
                is ExtractedMedia.Video -> HistoryMediaKind.Video
                is ExtractedMedia.Image -> HistoryMediaKind.Image
                is ExtractedMedia.Audio -> HistoryMediaKind.Audio
            }
        }.distinct()
        return kinds.singleOrNull() ?: HistoryMediaKind.Mixed
    }

    private fun String?.cleaned(maxLength: Int): String? = this
        ?.trim()
        ?.filterNot { it == NUL }
        ?.take(maxLength)
        ?.takeIf(String::isNotEmpty)

    private const val MAX_PLATFORM_LENGTH = 100
    private const val MAX_TITLE_LENGTH = 500
    private const val MAX_AUTHOR_LENGTH = 200
    private const val NUL = '\u0000'
}

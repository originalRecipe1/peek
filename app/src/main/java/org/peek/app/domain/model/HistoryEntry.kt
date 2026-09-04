package org.peek.app.domain.model

data class HistoryEntry(
    val id: Long,
    val sourceUrl: String,
    val platform: String?,
    val title: String?,
    val author: String?,
    val mediaKind: HistoryMediaKind,
    val mediaCount: Int,
    val durationSeconds: Long?,
    val viewedAtEpochMillis: Long,
)

enum class HistoryMediaKind {
    Video,
    Image,
    Audio,
    Gallery,
    Mixed,
}

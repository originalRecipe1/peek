package io.github.originalrecipe1.unfurlit.data.repository

import android.content.Context
import io.github.originalrecipe1.unfurlit.data.extractor.ytdlp.YtDlpMediaExtractor
import io.github.originalrecipe1.unfurlit.data.history.SqliteHistoryRepository
import io.github.originalrecipe1.unfurlit.domain.repository.DefaultMediaRepository
import io.github.originalrecipe1.unfurlit.domain.repository.HistoryRepository
import io.github.originalrecipe1.unfurlit.domain.repository.MediaRepository

object RepositoryFactory {
    fun mediaRepository(context: Context): MediaRepository =
        DefaultMediaRepository(YtDlpMediaExtractor(context))

    fun historyRepository(context: Context): HistoryRepository =
        historyRepository ?: synchronized(this) {
            historyRepository ?: SqliteHistoryRepository(context).also {
                historyRepository = it
            }
        }

    @Volatile
    private var historyRepository: HistoryRepository? = null
}

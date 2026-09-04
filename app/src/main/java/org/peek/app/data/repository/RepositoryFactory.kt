package org.peek.app.data.repository

import android.content.Context
import org.peek.app.data.extractor.ytdlp.YtDlpMediaExtractor
import org.peek.app.data.history.SqliteHistoryRepository
import org.peek.app.domain.repository.DefaultMediaRepository
import org.peek.app.domain.repository.HistoryRepository
import org.peek.app.domain.repository.MediaRepository

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

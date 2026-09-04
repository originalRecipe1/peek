package org.peek.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.peek.app.domain.model.ExtractionResult
import org.peek.app.domain.model.HistoryEntry

interface HistoryRepository {
    fun observeHistory(): Flow<List<HistoryEntry>>

    suspend fun recordView(result: ExtractionResult)

    suspend fun remove(id: Long)

    suspend fun clear()
}

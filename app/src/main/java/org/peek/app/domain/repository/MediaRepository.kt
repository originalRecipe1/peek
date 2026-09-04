package org.peek.app.domain.repository

import org.peek.app.domain.extractor.MediaExtractor
import org.peek.app.domain.model.ExtractionResult

interface MediaRepository {
    suspend fun open(url: String): ExtractionResult
}

class DefaultMediaRepository(
    private val extractor: MediaExtractor,
) : MediaRepository {
    override suspend fun open(url: String): ExtractionResult = extractor.extract(url)
}

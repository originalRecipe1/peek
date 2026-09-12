package io.github.originalrecipe1.unfurlit.domain.repository

import io.github.originalrecipe1.unfurlit.domain.extractor.MediaExtractor
import io.github.originalrecipe1.unfurlit.domain.model.ExtractionResult

interface MediaRepository {
    suspend fun open(url: String): ExtractionResult
}

class DefaultMediaRepository(
    private val extractor: MediaExtractor,
) : MediaRepository {
    override suspend fun open(url: String): ExtractionResult = extractor.extract(url)
}

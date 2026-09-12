package io.github.originalrecipe1.unfurlit.domain.extractor

import io.github.originalrecipe1.unfurlit.domain.model.ExtractionResult

fun interface MediaExtractor {
    suspend fun extract(url: String): ExtractionResult
}

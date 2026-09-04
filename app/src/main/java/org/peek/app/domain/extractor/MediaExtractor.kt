package org.peek.app.domain.extractor

import org.peek.app.domain.model.ExtractionResult

fun interface MediaExtractor {
    suspend fun extract(url: String): ExtractionResult
}

package org.peek.app.domain.model

sealed interface ExtractionError {
    data object UnsupportedUrl : ExtractionError
    data object MediaUnavailable : ExtractionError
    data object AuthenticationRequired : ExtractionError
    data object NetworkFailure : ExtractionError
    data object ExtractionFailed : ExtractionError
}

class ExtractionException(
    val error: ExtractionError,
    cause: Throwable? = null,
) : Exception(error.userMessage, cause)

val ExtractionError.userMessage: String
    get() = when (this) {
        ExtractionError.UnsupportedUrl -> "This URL is not supported."
        ExtractionError.MediaUnavailable -> "This media is unavailable."
        ExtractionError.AuthenticationRequired -> "This media requires an account or cookies."
        ExtractionError.NetworkFailure -> "The media service could not be reached."
        ExtractionError.ExtractionFailed -> "Could not open this media."
    }

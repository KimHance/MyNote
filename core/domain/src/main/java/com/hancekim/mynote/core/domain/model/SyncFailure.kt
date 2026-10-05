package com.hancekim.mynote.core.domain.model

sealed class SyncFailure(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Offline(cause: Throwable? = null) : SyncFailure("offline", cause)

    class TokenRejected(cause: Throwable? = null) : SyncFailure("token rejected", cause)

    class AccessDenied(cause: Throwable? = null) : SyncFailure("access denied", cause)

    class RateLimited(cause: Throwable? = null) : SyncFailure("rate limited", cause)

    class Server(cause: Throwable? = null) : SyncFailure("server error", cause)

    class Format(cause: Throwable? = null) : SyncFailure("manifest format", cause)
}

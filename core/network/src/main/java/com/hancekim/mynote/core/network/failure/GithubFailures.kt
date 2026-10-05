package com.hancekim.mynote.core.network.failure

import com.hancekim.mynote.core.domain.model.SyncFailure
import kotlinx.serialization.SerializationException
import okhttp3.Headers
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException

internal enum class HttpFailureKind { TokenRejected, NotFound, AccessDenied, RateLimited, Server }

internal fun classifyHttp(
    code: Int,
    headers: Headers,
): HttpFailureKind =
    when {
        code == 429 -> {
            HttpFailureKind.RateLimited
        }

        code == 403 && (headers["x-ratelimit-remaining"]?.trim() == "0" || headers["retry-after"] != null) -> {
            HttpFailureKind.RateLimited
        }

        code == 401 -> {
            HttpFailureKind.TokenRejected
        }

        code == 404 -> {
            HttpFailureKind.NotFound
        }

        code == 403 || code == 451 -> {
            HttpFailureKind.AccessDenied
        }

        else -> {
            HttpFailureKind.Server
        }
    }

internal fun syncFailureOf(failure: Throwable): Throwable =
    when (failure) {
        is SyncFailure -> {
            failure
        }

        is HttpException -> {
            when (classifyHttp(failure.code(), failure.response()?.headers() ?: Headers.headersOf())) {
                HttpFailureKind.TokenRejected -> SyncFailure.TokenRejected(failure)
                HttpFailureKind.NotFound, HttpFailureKind.AccessDenied -> SyncFailure.AccessDenied(failure)
                HttpFailureKind.RateLimited -> SyncFailure.RateLimited(failure)
                HttpFailureKind.Server -> SyncFailure.Server(failure)
            }
        }

        is SerializationException -> {
            SyncFailure.Format(failure)
        }

        is InterruptedIOException -> {
            SyncFailure.Server(failure)
        }

        is IOException -> {
            SyncFailure.Offline(failure)
        }

        else -> {
            failure
        }
    }

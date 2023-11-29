package com.paoapps.blockedcache

sealed class FetcherResult<out Network : Any> {
    data class Data<Network : Any>(val value: Network, val origin: String? = null) : FetcherResult<Network>()
    sealed class Error : FetcherResult<Nothing>() {
        data class Exception(val error: Throwable) : Error()
        data class Message(val message: String) : Error()
    }
}

internal val FetcherResult.Error.throwable: Throwable?
    get() = when (this) {
        is FetcherResult.Error.Exception -> error
        is FetcherResult.Error.Message -> null
    }

inline fun <T: Any, R: Any> FetcherResult<T>.map(transform: (T) -> R): FetcherResult<R> {
    when (this) {
        is FetcherResult.Data -> {
            return FetcherResult.Data(transform(value), origin)
        }
        is FetcherResult.Error -> {
            return map()
        }
    }
}

inline fun <T: Any, R: Any> FetcherResult<T>.flatMap(transform: (T) -> FetcherResult<R>): FetcherResult<R> {
    when (this) {
        is FetcherResult.Data -> {
            return transform(value)
        }
        is FetcherResult.Error -> {
            return map()
        }
    }
}

fun <T: Any> FetcherResult<T>.isEmpty(): Boolean {
    return when (this) {
        is FetcherResult.Data -> {
            value is List<*> && value.isEmpty()
        }
        is FetcherResult.Error -> {
            false
        }
    }
}

fun FetcherResult.Error.map(): FetcherResult.Error = when(this) {
    is FetcherResult.Error.Exception -> FetcherResult.Error.Exception(error)
    is FetcherResult.Error.Message -> FetcherResult.Error.Message(message)
}

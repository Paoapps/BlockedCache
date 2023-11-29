package com.paoapps.blockedcache

/**
 *
 */
sealed class CacheResult<out T> {

    object Empty: CacheResult<Nothing>()

    data class Offline<T>(val staleData: T? = null, val creationTimeStaleData: Long? = null): CacheResult<T>()

    /**
     * Indicaties the data is loading.
     */
    data class Loading<T>(val staleData: T? = null, val creationTimeStaleData: Long? = null) : CacheResult<T>()

    /**
     * Indicates a success.
     *
     * @property data The actual data.
     */
    data class Success<T>(val data: T) : CacheResult<T>()

    val actualOrStaleData: T?
        get() = when (this) {
            is Success -> this.data
            is Loading -> this.staleData
            is Error -> this.staleData
            is Offline -> this.staleData
            is Empty -> null
        }

    val isLoading: Boolean get() = this is Loading

    fun <D> map(transform: (T) -> (D)): CacheResult<D> = when(this) {
        is Loading -> Loading(staleData?.let(transform), creationTimeStaleData)
        is Success -> Success(transform(data))
        is Error -> Error(failure.map(), staleData?.let(transform), creationTimeStaleData)
        is Empty -> Empty
        is Offline -> Offline(staleData?.let(transform), creationTimeStaleData)
    }

    fun <D> flatMap(transform: (T) -> (CacheResult<D>)): CacheResult<D> = when(this) {
        is Loading -> Loading(staleData?.let(transform)?.actualOrStaleData, creationTimeStaleData)
        is Success -> transform(data)
        is Error -> Error(failure.map(), staleData?.let(transform)?.actualOrStaleData, creationTimeStaleData)
        is Empty -> Empty
        is Offline -> Offline(staleData?.let(transform)?.actualOrStaleData, creationTimeStaleData)
    }

    fun <D> mapNotNull(transform: (T) -> (D?)): CacheResult<D> = when(this) {
        is Loading -> Loading(staleData?.let(transform), creationTimeStaleData)
        is Success -> transform(data)?.let { Success(it) } ?: Empty
        is Error -> Error(failure.map(), staleData?.let(transform), creationTimeStaleData)
        is Empty -> Empty
        is Offline -> Offline(staleData?.let(transform), creationTimeStaleData)
    }

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Error

    operator fun <X> plus(other: CacheResult<X>): CacheResult<Pair<T?, X?>> {
        return when {
            this is Error -> Error(failure = failure.map(), staleData = Pair(staleData, other.actualOrStaleData), creationTimeStaleData = creationTimeStaleData)
            other is Error -> Error(other.failure.map(), Pair(this.actualOrStaleData, other.staleData), other.creationTimeStaleData)
            this is Loading || other is Loading -> Loading(
                null,
                0
            )
            else -> Success(
                Pair(
                    this.actualOrStaleData,
                    other.actualOrStaleData
                )
            )
        }
    }

    data class Error<T>(
        val failure: FetcherResult.Error,
        val staleData: T? = null,
        val creationTimeStaleData: Long? = null
    ) : CacheResult<T>() {

        override fun toString(): String {
            return "Response failure: $failure"
        }
    }
}

fun <T: Any> FetcherResult<T>.asCommonDataContainer(): CacheResult<T> = when (this) {
    is FetcherResult.Data -> {
        CacheResult.Success<T>(value)
    }
    is FetcherResult.Error -> {
        CacheResult.Error(this)
    }
}

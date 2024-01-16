package com.paoapps.blockedcache

/**
 * Represents the various states and outcomes of a cache operation.
 * This sealed class encapsulates different result states like success, loading, error, and others.
 *
 * @param T The type of data involved in the cache operation.
 */
sealed class CacheResult<out T> {

    /**
     * Represents an empty result, indicating no data is present.
     */
    object Empty: CacheResult<Nothing>()

    /**
     * Represents an offline state with potential stale data.
     *
     * @property staleData Optionally holds stale data if available.
     * @property creationTimeStaleData The creation time of the stale data, if available.
     */
    data class Offline<T>(val staleData: T? = null, val creationTimeStaleData: Long? = null): CacheResult<T>()

    /**
     * Represents a loading state, potentially with stale data.
     *
     * @property staleData Optionally holds stale data if available.
     * @property creationTimeStaleData The creation time of the stale data, if available.
     */
    data class Loading<T>(val staleData: T? = null, val creationTimeStaleData: Long? = null) : CacheResult<T>()

    /**
     * Represents a successful result with data.
     *
     * @property data The actual data fetched or retrieved from the cache.
     */
    data class Success<T>(val data: T) : CacheResult<T>()

    /**
     * Provides either the actual data in case of success, or the stale data in other states.
     * Returns null for the Empty state.
     */
    val actualOrStaleData: T?
        get() = when (this) {
            is Success -> this.data
            is Loading -> this.staleData
            is Error -> this.staleData
            is Offline -> this.staleData
            is Empty -> null
        }

    /**
     * Indicates whether the cache result is currently in the loading state.
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * Transforms the data within the cache result to a different type.
     *
     * @param D The type to which the data is to be transformed.
     * @param transform A transformation function applied to the data.
     * @return A new [CacheResult] instance with the transformed data.
     */
    fun <D> map(transform: (T) -> (D)): CacheResult<D> = when(this) {
        is Loading -> Loading(staleData?.let(transform), creationTimeStaleData)
        is Success -> Success(transform(data))
        is Error -> Error(failure.map(), staleData?.let(transform), creationTimeStaleData)
        is Empty -> Empty
        is Offline -> Offline(staleData?.let(transform), creationTimeStaleData)
    }

    /**
     * Transforms the data within the cache result to a different type, encapsulated within another [CacheResult].
     *
     * @param D The type to which the data is to be transformed.
     * @param transform A transformation function that returns a new [CacheResult] instance.
     * @return A new [CacheResult] instance based on the transformation applied.
     */
    fun <D> flatMap(transform: (T) -> (CacheResult<D>)): CacheResult<D> = when(this) {
        is Loading -> Loading(staleData?.let(transform)?.actualOrStaleData, creationTimeStaleData)
        is Success -> transform(data)
        is Error -> Error(failure.map(), staleData?.let(transform)?.actualOrStaleData, creationTimeStaleData)
        is Empty -> Empty
        is Offline -> Offline(staleData?.let(transform)?.actualOrStaleData, creationTimeStaleData)
    }

    /**
     * Transforms the data within the cache result to a different type, ignoring null transformations.
     *
     * @param D The type to which the data is to be transformed.
     * @param transform A transformation function that returns a nullable result.
     * @return A new [CacheResult] instance with the transformed data, or Empty if the transformation result is null.
     */
    fun <D> mapNotNull(transform: (T) -> (D?)): CacheResult<D> = when(this) {
        is Loading -> Loading(staleData?.let(transform), creationTimeStaleData)
        is Success -> transform(data)?.let { Success(it) } ?: Empty
        is Error -> Error(failure.map(), staleData?.let(transform), creationTimeStaleData)
        is Empty -> Empty
        is Offline -> Offline(staleData?.let(transform), creationTimeStaleData)
    }

    /**
     * Indicates whether the cache result represents a successful state.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Indicates whether the cache result represents an error state.
     */
    val isFailure: Boolean get() = this is Error

    /**
     * Combines this cache result with another, creating a pair of their data.
     *
     * @param X The type of data in the other cache result.
     * @param other Another cache result to be combined with this one.
     * @return A new [CacheResult] instance holding a pair of data from both cache results.
     */
    operator fun <X> plus(other: CacheResult<X>): CacheResult<Pair<T?, X?>> {
        return when {
            this is Error -> Error(failure = failure.map(), staleData = Pair(staleData, other.actualOrStaleData), creationTimeStaleData = creationTimeStaleData)
            other is Error -> Error(other.failure.map(), Pair(this.actualOrStaleData, other.staleData), other.creationTimeStaleData)
            this is Loading || other is Loading -> Loading(
                null,
                0
            )
            this is Empty && other is Empty -> Empty
            else -> Success(
                Pair(
                    this.actualOrStaleData,
                    other.actualOrStaleData
                )
            )
        }
    }

    /**
     * Represents an error state in a cache operation.
     *
     * @property failure The error that occurred during the cache operation.
     * @property staleData Optionally holds stale data if available.
     * @property creationTimeStaleData The creation time of the stale data, if available.
     */
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

/**
 * Converts a [FetcherResult] to a corresponding [CacheResult].
 *
 * @param T The type of data involved in the operation.
 * @return The corresponding [CacheResult] based on the [FetcherResult].
 */
fun <T: Any> FetcherResult<T>.asCacheResult(): CacheResult<T> = when (this) {
    is FetcherResult.Data -> {
        CacheResult.Success<T>(value)
    }
    is FetcherResult.Error -> {
        CacheResult.Error(this)
    }
}

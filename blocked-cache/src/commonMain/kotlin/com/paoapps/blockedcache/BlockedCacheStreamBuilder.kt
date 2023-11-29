package com.paoapps.blockedcache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A builder class for creating instances of [BlockedCacheStream]. This class provides a fluent interface
 * to configure and build a stream around a [BlockedCache] with specified fetcher, update, and condition logic.
 *
 * @param T The type of data managed by the cache.
 * @property cache The [BlockedCache] instance to be used for data storage and retrieval.
 * @property fetcher A [Fetcher] instance, defining how new data is fetched.
 */
class BlockedCacheStreamBuilder<T: Any>(
    private var cache: BlockedCache<T>,
    private var fetcher: Fetcher<T>,
) {
    private var updateData: ((BlockedCacheData<T>) -> Unit) = { }
    private var condition: Flow<Boolean> = flowOf(true)

    /**
     * Represents a fetching logic encapsulating a suspend function to fetch data.
     *
     * @param T The type of data to be fetched.
     * @property fetcher A suspend function that returns a [FetcherResult] representing the fetched data.
     */
    data class Fetcher<T: Any>(
        val fetcher: suspend () -> FetcherResult<T>,
    ) {
        companion object {
            fun <T: Any> ofResult(fetcher: suspend () -> FetcherResult<T>) = Fetcher(fetcher)
            fun <T: Any> of(fetcher: suspend () -> T) = Fetcher {
                try {
                    FetcherResult.Data(fetcher())
                } catch (e: Exception) {
                    FetcherResult.Error.Exception(e)
                }
            }
        }
    }

    /**
     * Sets the function to update the cache data.
     *
     * @param updateData A function to update the cache with new data.
     * @return The current instance of [BlockedCacheStreamBuilder] for fluent configuration.
     */
    fun updateData(updateData: (BlockedCacheData<T>) -> Unit) = apply {
        this.updateData = updateData
    }

    companion object {
        fun <T: Any> from(
            cache: BlockedCache<T>,
            fetcher: Fetcher<T>
        ) = BlockedCacheStreamBuilder(cache, fetcher)
    }

    /**
     * Builds and returns a [BlockedCacheStream] with the configured settings.
     *
     * @return An instance of [BlockedCacheStream] configured with the provided cache, fetcher, and update logic.
     */
    fun build(): BlockedCacheStream<T> {
        return BlockedCacheStream(
            cache = cache,
            fetcher = fetcher,
            updateData = { updateData(it) },
            condition = condition
        )
    }
}

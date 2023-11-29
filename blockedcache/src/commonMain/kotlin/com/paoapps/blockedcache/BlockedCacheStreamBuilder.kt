package com.paoapps.blockedcache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class BlockedCacheStreamBuilder<T: Any>(
    private var cache: BlockedCache<T>,
    private var fetcher: Fetcher<T>,
) {
    private var updateData: ((BlockedCacheData<T>) -> Unit) = { }
    private var condition: Flow<Boolean> = flowOf(true)

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

    data class Refresher(
        val fetch: Fetch,
    ) {
        companion object {
            fun of(fetch: Fetch) = Refresher(fetch)
            fun refresh(forceRefresh: Boolean) = Refresher(if (forceRefresh) Fetch.FORCE else Fetch.CACHE)
        }
    }

    fun updateData(updateData: (BlockedCacheData<T>) -> Unit) = apply {
        this.updateData = updateData
    }

    companion object {
        fun <T: Any> from(
            cache: BlockedCache<T>,
            fetcher: Fetcher<T>
        ) = BlockedCacheStreamBuilder(cache, fetcher)
    }

    fun build(): BlockedCacheStream<T> {
        return BlockedCacheStream(
            cache = cache,
            fetcher = fetcher,
            updateData = { updateData(it) },
            condition = condition
        )
    }
}

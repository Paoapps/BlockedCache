package com.paoapps.blockedcache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BlockedCacheStream<T: Any>(
    private val cache: BlockedCache<T>,
    private val fetcher: BlockedCacheStreamBuilder.Fetcher<T>,
    private val updateData: ((BlockedCacheData<T>) -> Unit),
    private val condition: Flow<Boolean>
) {
    fun stream(forceRefresh: Boolean) = cache.getData(
        forceRefresh = forceRefresh,
        fetcher = fetcher.fetcher,
        updateData = updateData,
        condition = condition,
    )

    fun stream(fetch: Fetch) = cache.getData(
        forceRefresh = fetch == Fetch.FORCE,
        fetcher = fetcher.fetcher,
        updateData = updateData,
        condition = condition.map { it && fetch != Fetch.NO_FETCH },
    )
}

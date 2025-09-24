@file:OptIn(ExperimentalTime::class)

package com.paoapps.blockedcache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

/**
 * Manages the streaming of data from a [BlockedCache], providing an interface to fetch and update the cache.
 * This class acts as a wrapper around [BlockedCache], streamlining the process of retrieving and updating
 * cached data based on specified conditions and fetch strategies.
 *
 * @param T The type of data managed by the cache.
 * @property cache The [BlockedCache] instance used to store and manage the data.
 * @property fetcher A [BlockedCacheStreamBuilder.Fetcher] instance, defining how new data is fetched.
 * @property updateData A function to update the cache with new data.
 * @property condition A flow representing additional conditions to control data fetching.
 */
data class BlockedCacheStream<T: Any>(
    private val cache: BlockedCache<T>,
    private val fetcher: BlockedCacheStreamBuilder.Fetcher<T>,
    private val updateData: ((BlockedCacheData<T>) -> Unit),
    private val condition: Flow<Boolean>
) {
    /**
     * Starts streaming data from the cache, optionally forcing a refresh.
     *
     * @param forceRefresh If true, forces a refresh of the data, ignoring cache freshness.
     * @return A flow of [CacheResult] representing the state and data of the cache.
     */
    fun stream(forceRefresh: Boolean = false) = cache.getData(
        forceRefresh = forceRefresh,
        fetcher = fetcher.fetcher,
        updateData = updateData,
        condition = condition,
    )

    /**
     * Starts streaming data from the cache based on a specified fetch strategy.
     *
     * @param fetch An instance of [Fetch] enum to define the fetch strategy.
     * @return A flow of [CacheResult] representing the state and data of the cache.
     */
    fun stream(fetch: Fetch) = cache.getData(
        forceRefresh = fetch is Fetch.Force,
        forceRefreshDelay = (fetch as? Fetch.Force)?.minimumDelay,
        fetcher = fetcher.fetcher,
        updateData = updateData,
        predicate = { _, _ ->
            if (fetch is Fetch.Cache) {
                !fetch.ignoreExpiration // never fetch from network if ignoreExpiration is true
            } else true
        },
        condition = condition.map { it && fetch != Fetch.NoFetch },
    )
}

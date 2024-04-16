package com.paoapps.blockedcache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.random.Random

sealed class Fetch {
    /**
     * Force fetch from network.
     * minimumDelay: Minimum delay between fetches in milliseconds. Default is 5 seconds.
     */
    data class Force(val minimumDelay: Long = 5000L) : Fetch()

    /**
     * Fetch from cache if available, otherwise fetch from network.
     *
     * ignoreExpiration: If true, fetch from cache even if the cache is expired.
     */
    data class Cache(val ignoreExpiration: Boolean = false) : Fetch()

    /**
     * Fetch from cache if available, otherwise do not fetch.
     */
    object NoFetch : Fetch()
}

sealed class FetchRefreshTrigger {
    data class FromCache(private val id: String = Random.nextInt().toString()) : FetchRefreshTrigger()

    /**
     * Every instance has a unique id so the refresh StateFlow sees it as a new value. 
     * StateFlow does not emit the same value twice if the current value is the same as the new value.
     */
    data class Refresh(private val id: String = Random.nextInt().toString()) : FetchRefreshTrigger()

    companion object {
        fun refreshTriggerFlow(): MutableStateFlow<FetchRefreshTrigger> = MutableStateFlow(
            FromCache()
        )
    }
}

fun <R> Flow<FetchRefreshTrigger>.createRefreshableFlow(data: suspend (refresh: Boolean) -> Flow<R>): Flow<R> {
    return flatMapLatest { trigger ->
        val refresh = trigger is FetchRefreshTrigger.Refresh
        data(refresh)
    }.distinctUntilChanged()
}

fun <R> Flow<FetchRefreshTrigger>.createRefreshableFetchFlow(data: (refresh: Fetch) -> Flow<R>) = createRefreshableFetchFlow(default = Fetch.Cache(), data = data)

fun <R> Flow<FetchRefreshTrigger>.createRefreshableFetchFlow(default: Fetch, data: (refresh: Fetch) -> Flow<R>): Flow<R> =
    createRefreshableFlow { data(if (it) Fetch.Force() else default) }

fun MutableStateFlow<FetchRefreshTrigger>.refresh() {
    value = FetchRefreshTrigger.Refresh()
}

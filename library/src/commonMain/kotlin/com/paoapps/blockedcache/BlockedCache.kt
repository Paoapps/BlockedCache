@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class, ExperimentalTime::class)

package com.paoapps.blockedcache

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Logger.Companion.setLogWriters
import com.paoapps.blockedcache.utils.DatetimeNowProvider
import com.paoapps.blockedcache.utils.NowProvider
import com.paoapps.blockedcache.utils.network.NetworkStatus
import com.paoapps.blockedcache.utils.network.NetworkStatusMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

/**
 * A cache mechanism for data, which can be refreshed and expired based on time and network status.
 *
 * It avoids multiple threads to retrieve the same information from a backend when no
 * data is available in the cache.
 * The cache is based on Flow which enables data to change if the data within the cache changes.
 * When requesting data old data will be emitted in a Loading object. When new information becomes
 * available the new data will be emitted in an success object.
 *
 * @param T The type of data to be cached.
 * @property refreshTime Time in milliseconds after which the cache will consider data stale and try to refresh.
 * @property expireTime Optional time in milliseconds after which the cached data is considered expired.
 * @property trigger A flow of events that can trigger a cache refresh.
 * @property dataFlow A flow of cached data.
 * @property networkStatusFlow A flow representing the network status. It's determined if fetching of new data failed due to the network being unavailable. In order to take advantage of this feature you must update NetworkStatusMonitor.networkStatus with the current network status.
 * @property nowProvider A provider for the current time. Useful for testing.
 * @property name A name identifier for the cache, useful for debugging.
 * @property isDebugEnabled Flag to enable or disable debug logging.
 */
class BlockedCache<T: Any>(
    private val refreshTime: Long,
    private val expireTime: Long? = null,
    private val trigger: Flow<Unit> = flowOf(Unit),
    private val dataFlow: Flow<BlockedCacheData<T>>,
    private val networkStatusFlow: Flow<NetworkStatus> = NetworkStatusMonitor.networkStatus,
    private val nowProvider: NowProvider = DatetimeNowProvider(),
    private val name: String = "genericBlockedCache",
    private val isDebugEnabled: Boolean = false,
) {
    constructor(
        refreshTime: Duration,
        expireTime: Duration? = null,
        trigger: Flow<Unit> = flowOf(Unit),
        dataFlow: Flow<BlockedCacheData<T>>,
        networkStatusFlow: Flow<NetworkStatus> = NetworkStatusMonitor.networkStatus,
        nowProvider: NowProvider = DatetimeNowProvider(),
        name: String = "genericBlockedCache",
        isDebugEnabled: Boolean = false
    ): this(
        refreshTime = refreshTime.inWholeMilliseconds,
        expireTime = expireTime?.inWholeMilliseconds,
        trigger = trigger,
        dataFlow = dataFlow,
        networkStatusFlow = networkStatusFlow,
        nowProvider = nowProvider,
        name = name,
        isDebugEnabled = isDebugEnabled
    )

    data class RefreshTrigger(val random: String = Random.nextInt().toString(), val forceRefresh: Boolean = false)

    private val refreshTriggerState = MutableStateFlow(RefreshTrigger())
    private val refreshTrigger = combine(trigger, refreshTriggerState, networkStatusFlow.debounce(100.milliseconds).distinctUntilChanged()) { _, trigger, _ -> RefreshTrigger(forceRefresh = trigger.forceRefresh) }

    private val mutex = Mutex()
    private val lastForceRefresh: MutableStateFlow<Long> = MutableStateFlow(0)

    private val logger = Logger.withTag("BlockedCache($name)").apply {
        setLogWriters(listOf(CommonWriter()))
    }

    private fun debugCache(message: String) {
        if (isDebugEnabled) {
            logger.d(message)
        }
    }

    /**
     * Retrieves data from the cache. It performs a network fetch if the data is considered stale or expired,
     * or when a force refresh is triggered.
     *
     * @param forceRefresh Forces a data refresh irrespective of current cache status.
     * @property forceRefreshDelay Time in milliseconds after which a new force refresh can be performed. Defaults to 5 seconds.
     * @param predicate A predicate to determine if the data should be refreshed based on its value and creation time.
     * @param condition A flow representing additional conditions to control data fetching.
     * @param fetcher A suspend function to fetch new data.
     * @param updateData A function to update the cache with new data.
     * @return A flow of [CacheResult], representing the state and data of the cache.
     */
    fun getData(
        forceRefresh: Boolean = false,
        forceRefreshDelay: Long? = null,
        predicate: (T, Instant) -> Boolean = { _, _ -> true },
        condition: Flow<Boolean> = flowOf(true),
        fetcher: suspend () -> FetcherResult<T>,
        updateData: (BlockedCacheData<T>) -> Unit
    ): Flow<CacheResult<T>> {
        debugCache("start")

        var lockedByMe = false

        val responseFlow: Flow<CacheResult<T>> = refreshTrigger.flatMapLatest { trigger ->
            fun getData(): Flow<CacheResult<T>> {
                return dataFlow.take(1).transformLatest { cacheData ->
                    debugCache("cacheData = $cacheData")
                    try {
                        debugCache("within try")
                        val result = cacheData.data
                        if (result == null || (predicate(result, cacheData.creationTime?.let { Instant.fromEpochMilliseconds(it) } ?: Clock.System.now()) && shouldFetchNewData(
                                cacheData,
                                forceRefresh || trigger.forceRefresh,
                                forceRefreshDelay ?: FORCE_REFRESH_DELAY.toLong()
                            ))
                        ) {
                            emit(CacheResult.Loading(result, 0))
                            debugCache("Loading")

                            val response = fetcher.invoke()

                            if (response is FetcherResult.Data) {
                                updateData(BlockedCacheData(response.value, nowProvider.now()))
                            } else if (response is FetcherResult.Error && response.throwable !is CancellationException && isExpired(cacheData)) {
                                updateData(BlockedCacheData(null, null))
                            }

                            if (forceRefresh) lastForceRefresh.value = nowProvider.now()
                            when (response) {
                                is FetcherResult.Data -> {
                                    emit(CacheResult.Success(response.value))
                                    debugCache("new data Success")
                                }
                                is FetcherResult.Error -> {
                                    emit(
                                        CacheResult.Error(
                                            response,
                                            cacheData.data,
                                            cacheData.creationTime
                                        )
                                    )
                                    debugCache("new data Failure")
                                }
                            }
                        } else {
                            emit(CacheResult.Success(result))
                            debugCache("cached data Success (${cacheData.creationTime})")
                        }

                    } finally {
                        debugCache("🟢 unlock")
                        mutex.unlock()
                        lockedByMe = false
                    }
                }.onCompletion {
                    if (mutex.isLocked && lockedByMe) {
                        debugCache("🟢 unlock in completion")
                        mutex.unlock()
                        lockedByMe = false
                    }
                }
            }

            condition.distinctUntilChanged().flatMapLatest { shouldFetch ->
                debugCache("shouldFetch = $shouldFetch")
                if (!shouldFetch) {
                    return@flatMapLatest dataFlow.take(1).transformLatest { cacheData ->
                        emit(cacheData.asCommonDataContainer())
                    }
                }

                if (!mutex.tryLock()) {
                    debugCache("locked")
                    flow {
                        emit(CacheResult.Loading(null, 0))

                        mutex.lock()
                        lockedByMe = true
                        debugCache("🔴 lock")

                        getData().collect { value ->
                            try {
                                debugCache("within collect")

                                emit(value)
                            } catch (e: Throwable) {
                                debugCache("🔴 e: $e")
                            }
                        }
                    }
                } else {
                    lockedByMe = true
                    debugCache("🔴 lock")
                    getData()
                }
            }
        }

        return responseFlow.flatMapLatest { state ->
            combine(dataFlow, networkStatusFlow) { data, networkStatus ->
                when (state) {
                    is CacheResult.Loading -> CacheResult.Loading(data.data, state.creationTimeStaleData)
                    is CacheResult.Success -> data.asCommonDataContainer()
                    is CacheResult.Error -> when(networkStatus) {
                        NetworkStatus.AVAILABLE, NetworkStatus.UNKNOWN -> state.copy(staleData = data.data, creationTimeStaleData = data.creationTime)
                        NetworkStatus.UNAVAILABLE -> CacheResult.Offline(staleData = data.data, creationTimeStaleData = data.creationTime)
                    }
                    is CacheResult.Empty -> CacheResult.Empty
                    is CacheResult.Offline -> CacheResult.Offline(data.data, state.creationTimeStaleData)
                }
            }
        }.onCompletion {
            debugCache("🟢 unlock on completion")
            if (mutex.isLocked && lockedByMe) {
                mutex.unlock()
            }
        }
    }

    private fun shouldFetchNewData(
        cacheData: BlockedCacheData<T>,
        forceRefresh: Boolean,
        forceRefreshDelay: Long
    ): Boolean {
        val now = nowProvider.now()
        return (shouldRefresh(cacheData, refreshTime, now)
                || shouldforceRefresh(cacheData, forceRefresh, forceRefreshDelay, now))
    }

    private fun shouldforceRefresh(
        cacheData: BlockedCacheData<T>,
        forceRefresh: Boolean,
        forceRefreshDelay: Long,
        now: Long): Boolean {
        return forceRefresh && (lastForceRefresh.value + forceRefreshDelay) < now && (cacheData.creationTime ?: 0L) + forceRefreshDelay < now
    }

    private fun shouldRefresh(
        cacheData: BlockedCacheData<T>,
        expireTimeMillis: Long,
        now: Long
    ): Boolean {
        return (cacheData.creationTime ?: 0L) + expireTimeMillis < now
    }

    private fun isExpired(cacheData: BlockedCacheData<T>): Boolean =
        expireTime != null && (cacheData.creationTime ?: 0L) + expireTime < nowProvider.now()

    /**
     * Triggers a cache refresh. If forceRefresh is true, the cache will attempt to fetch new data
     * even if the current data is not stale or expired.
     *
     * @param forceRefresh Flag to force a data refresh.
     */
    fun refresh(forceRefresh: Boolean = true) {
        refreshTriggerState.value = RefreshTrigger(forceRefresh = forceRefresh)
    }

    companion object {
        private const val FORCE_REFRESH_DELAY = 5000 // After 5 seconds a new force refresh can be performed
    }
}

fun <T> BlockedCacheData<T>.asCommonDataContainer(): CacheResult<T> =
        data?.let { CacheResult.Success(it) } ?: CacheResult.Empty

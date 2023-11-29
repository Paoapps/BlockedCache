package com.paoapps.blockedcache

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import com.paoapps.blockedcache.utils.DatetimeNowProvider
import com.paoapps.blockedcache.utils.NowProvider
import com.paoapps.blockedcache.utils.network.NetworkStatus
import com.paoapps.blockedcache.utils.network.NetworkStatusMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.jvm.JvmName
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A cache which avoids multiple threads to retrieve the same information from a backend when no
 * data is available in the cache.
 * The cache is based on Flow which enables data to change if the data within the cache changes.
 * When requesting data old data will be emitted in a Loading object. When new information becomes
 * available the new data will be emitted in an success object.
 *
 * @param refreshTime The time in millis after which the cache should be refreshed.
 * @param expireTime The expire time of the item in millis.
 * @param trigger A trigger to refresh the cache. This can be used to refresh the cache
 * @param dataFlow The data (or no data if it is not present) and additional information the cache needs.
 * @param networkStatusFlow The network status. This is used to determine if the cache should be refreshed.
 * @param nowProvider Provider of now(). Useful for unit testing.
 * @param name Can be used for debugging.
 * @param isDebugEnabled Can be used for debugging.
 */

class BlockedCache<T: Any>(
    private val refreshTime: Long,
    private val expireTime: Long? = null,
    private val trigger: Flow<Unit> = flowOf(Unit),
    private val dataFlow: Flow<BlockedCacheData<T>>,
    private val networkStatusFlow: Flow<NetworkStatus> = NetworkStatusMonitor.networkStatus,
    private val nowProvider: NowProvider = DatetimeNowProvider(),
    private val name: String = "genericBlockedCache",
    private val isDebugEnabled: Boolean = false
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

    private val logger = Logger.apply {
        setLogWriters(listOf(CommonWriter()))
        setTag("BlockedCache($name)")
    }

    private fun debugCache(message: String) {
        if (isDebugEnabled) {
            logger.d(message)
        }
    }

    fun getData(
        forceRefresh: Boolean = false,
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
        forceRefresh: Boolean
    ): Boolean {
        val now = nowProvider.now()
        return (shouldRefresh(cacheData, refreshTime, now)
                || shouldforceRefresh(cacheData, forceRefresh, now))
    }

    private fun shouldforceRefresh(
        cacheData: BlockedCacheData<T>,
        forceRefresh: Boolean,
        now: Long): Boolean {
        return forceRefresh && (lastForceRefresh.value + FORCE_REFRESH_DELAY) < now && (cacheData.creationTime ?: 0L) + FORCE_REFRESH_DELAY < now
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

    fun refresh(forceRefresh: Boolean = true) {
        refreshTriggerState.value = RefreshTrigger(forceRefresh = forceRefresh)
    }

    companion object {
        private const val FORCE_REFRESH_DELAY = 5000 // After 5 seconds a new force refresh can be performed
    }
}

fun <T> BlockedCacheData<T>.asCommonDataContainer(): CacheResult<T> =
        data?.let { CacheResult.Success(it) } ?: CacheResult.Empty

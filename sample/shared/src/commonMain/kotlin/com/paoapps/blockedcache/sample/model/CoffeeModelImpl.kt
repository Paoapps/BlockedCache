package com.paoapps.blockedcache.sample.model

import com.paoapps.blockedcache.BlockedCache
import com.paoapps.blockedcache.BlockedCacheData
import com.paoapps.blockedcache.BlockedCacheStreamBuilder
import com.paoapps.blockedcache.sample.api.CoffeeApi
import com.paoapps.blockedcache.sample.api.impl.CoffeeApiImpl
import com.paoapps.blockedcache.sample.domain.Coffee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.minutes

class CoffeeModelImpl(
    private val api: CoffeeApi = CoffeeApiImpl()
): CoffeeModel {
    // This is the source of truth for the cache. Which in this case is in memory, but it could be
    // a database or a file.
    private val sourceOfTruth = MutableStateFlow(BlockedCacheData<List<Coffee>>())

    override val hotCoffeeStream = BlockedCacheStreamBuilder.from(
        cache = BlockedCache(
            refreshTime = 5.minutes,
            dataFlow = sourceOfTruth,
            name = "hotCoffee",
            isDebugEnabled = true
        ),
        fetcher = BlockedCacheStreamBuilder.Fetcher.of { api.hotCoffee() }
    )
        .updateData { sourceOfTruth.value = it }
        .build()
}

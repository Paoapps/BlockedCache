package com.paoapps.blockedcache.sample.model

import com.paoapps.blockedcache.CacheResult
import com.paoapps.blockedcache.Fetch
import com.paoapps.blockedcache.sample.domain.Coffee
import kotlinx.coroutines.flow.Flow

interface CoffeeModel {
    val hotCoffeeStream: BlockedCacheStream<List<Coffee>>
}

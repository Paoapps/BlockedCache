package com.paoapps.blockedcache.sample.model

import com.paoapps.blockedcache.BlockedCacheStream
import com.paoapps.blockedcache.sample.domain.Coffee

interface CoffeeModel {
    val hotCoffeeStream: BlockedCacheStream<List<Coffee>>
}

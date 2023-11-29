package com.paoapps.blockedcache.sample.api

import com.paoapps.blockedcache.sample.domain.Coffee

interface CoffeeApi {
    suspend fun hotCoffee(): List<Coffee>
}

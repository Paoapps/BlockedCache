package com.paoapps.blockedcache.sample.viewmodel

import com.paoapps.blockedcache.FetchRefreshTrigger
import com.paoapps.blockedcache.createRefreshableFetchFlow
import com.paoapps.blockedcache.refresh
import com.paoapps.blockedcache.sample.model.CoffeeModel
import kotlinx.coroutines.flow.map
import org.koin.core.component.inject

class HomeViewModelImpl: HomeViewModel() {

    private val coffeeModel: CoffeeModel by inject()

    private val refreshTrigger = FetchRefreshTrigger.refreshTriggerFlow()
    private val coffee = refreshTrigger.createRefreshableFetchFlow(coffeeModel.hotCoffeeStream::stream)

    override val output = coffee.map { coffeeCache ->
        Output(
            title = "Fifi sample app - browse coffee",
            items = coffeeCache.actualOrStaleData?.map { coffee ->
                Output.Item(
                    title = coffee.title,
                    description = coffee.description,
                    id = coffee.id
                )
            } ?: emptyList(),
        )
    }

    override fun refresh() {
        refreshTrigger.refresh()
    }
}

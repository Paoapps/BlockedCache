package com.paoapps.blockedcache.sample.viewmodel

import com.paoapps.blockedcache.FetchRefreshTrigger
import com.paoapps.blockedcache.createRefreshableFetchFlow
import com.paoapps.blockedcache.sample.model.CoffeeModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.component.inject

class CoffeeDetailViewModelImpl(
    private val id: Int,
): CoffeeDetailViewModel() {

    private val coffeeModel: CoffeeModel by inject()

    private val refreshTrigger = FetchRefreshTrigger.refreshTriggerFlow()
    private val coffee = refreshTrigger.createRefreshableFetchFlow(coffeeModel.hotCoffeeStream::stream).mapNotNull { it.actualOrStaleData?.firstOrNull { it.id == id }}

    override val output = coffee.map { coffee ->
        Output(
            title = coffee.title,
            description = coffee.description,
            ingredients = coffee.ingredients.takeIf { it.isNotEmpty() }?.let {
                 "Ingredients: ${it.joinToString(", ")}"
            } ?: "No ingredients",
        )
    }.distinctUntilChanged()
}

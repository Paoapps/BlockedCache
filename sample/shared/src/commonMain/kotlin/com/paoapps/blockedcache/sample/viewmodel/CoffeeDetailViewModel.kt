package com.paoapps.blockedcache.sample.viewmodel

import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

abstract class CoffeeDetailViewModel: KoinComponent {
    data class Output(
        val title: String,
        val description: String,
        val ingredients: String,
    )

    abstract val output: Flow<Output>
}
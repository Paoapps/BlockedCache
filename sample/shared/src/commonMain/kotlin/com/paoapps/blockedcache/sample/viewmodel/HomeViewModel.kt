package com.paoapps.blockedcache.sample.viewmodel

import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

abstract class HomeViewModel: KoinComponent {

    data class Output(
        val title: String,
        val items: List<Item>
    ) {

        data class Item(
            val title: String,
            val description: String,
            val id: Int
        )
    }

    abstract val output: Flow<Output>

    abstract fun refresh()
}

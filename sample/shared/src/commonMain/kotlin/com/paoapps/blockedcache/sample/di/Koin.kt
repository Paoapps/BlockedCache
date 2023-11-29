package com.paoapps.blockedcache.sample.di

import com.paoapps.blockedcache.sample.model.CoffeeModel
import com.paoapps.blockedcache.sample.model.CoffeeModelImpl
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * This initializes the FiFi framework and registers the app module with Koin.
 *
 * @param sharedAppModule The Koin module that is shared between client and server.
 */
fun initKoinApp(
    sharedAppModule: Module,
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(module {
        single<CoffeeModel> { CoffeeModelImpl() }
    }, sharedAppModule)
}


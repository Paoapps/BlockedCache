package com.paoapps.blockedcache.sample

import com.paoapps.blockedcache.sample.di.initKoinApp
import com.paoapps.blockedcache.sample.viewmodel.CoffeeDetailViewModel
import com.paoapps.blockedcache.sample.viewmodel.CoffeeDetailViewModelImpl
import com.paoapps.blockedcache.sample.viewmodel.HomeViewModel
import com.paoapps.blockedcache.sample.viewmodel.HomeViewModelImpl
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

class SharedApp: KoinComponent {

    val appModule = module {
        single<HomeViewModel> { HomeViewModelImpl() }
        single<CoffeeDetailViewModel> { (id: Int) -> CoffeeDetailViewModelImpl(id) }
    }

}

fun initApp(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    return initKoinApp(
        SharedApp().appModule,
    ) {
        appDeclaration()
    }
}

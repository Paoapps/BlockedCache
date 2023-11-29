package com.paoapps.blockedcache.sample.android

import android.app.Application
import com.paoapps.blockedcache.sample.android.composeui.detail.AndroidCoffeeDetailViewModel
import com.paoapps.blockedcache.sample.android.composeui.home.AndroidHomeViewModel
import com.paoapps.blockedcache.sample.initApp
import org.koin.android.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

class App: Application() {


    override fun onCreate() {
        super.onCreate()

        val koin = initApp {
            androidContext(this@App)
            modules(module {
                viewModel { (id: Int) -> AndroidCoffeeDetailViewModel(get { parametersOf(id) }) }
                viewModel { AndroidHomeViewModel(get()) }
            })
        }
        if (BuildConfig.DEBUG) koin.androidLogger(Level.ERROR)
    }

}

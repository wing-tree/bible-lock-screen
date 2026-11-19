package com.duke.orca.android.kotlin.biblelockscreen.application

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.duke.orca.android.kotlin.biblelockscreen.BuildConfig
import com.duke.orca.android.kotlin.biblelockscreen.ad.NativeAds
import com.duke.orca.android.kotlin.biblelockscreen.dailyverse.DailyVerseNotice
import com.duke.orca.android.kotlin.biblelockscreen.dailyverse.DailyVerseWidget
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.store.Billing
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        DataStore.init(this)

        AppCompatDelegate.setDefaultNightMode(DataStore.Display.getNightMode(this))

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Billing.init(this)
        NativeAds.init(this)
        DailyVerseNotice.schedule(this)
        DailyVerseWidget.refresh(this)
    }

    companion object {
        private lateinit var INSTANCE: MainApplication

        val applicationContext: Context
            get() = INSTANCE.applicationContext
    }
}
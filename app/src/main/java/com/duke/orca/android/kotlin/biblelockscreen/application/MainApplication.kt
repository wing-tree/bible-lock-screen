package com.duke.orca.android.kotlin.biblelockscreen.application

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.duke.orca.android.kotlin.biblelockscreen.BuildConfig
import com.duke.orca.android.kotlin.biblelockscreen.ad.NativeAds
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        DataStore.init(this)

        with(DataStore.Display.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(
                if (this) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            )
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        NativeAds.init(this)
    }

    companion object {
        private lateinit var INSTANCE: MainApplication

        val applicationContext: Context
            get() = INSTANCE.applicationContext
    }
}
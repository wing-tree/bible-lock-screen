package com.duke.orca.android.kotlin.biblelockscreen.datastore

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.duke.orca.android.kotlin.biblelockscreen.application.Application
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "${Application.PACKAGE_NAME}.settings")

object DataStore {
    fun getBoolean(context: Context, key: Preferences.Key<Boolean>, defValue: Boolean): Boolean = runBlocking {
        context.dataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    fun getInt(context: Context, key: Preferences.Key<Int>, defValue: Int): Int = runBlocking {
        context.dataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    fun getFloat(context: Context, key: Preferences.Key<Float>, defValue: Float): Float = runBlocking {
        context.dataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    object BibleVerse {
        fun getCurrentItem(context: Context) = getInt(context, PreferencesKeys.BibleVerse.currentItem, 0)

        fun putCurrentItem(context: Context, value: Int) = runBlocking {
            context.dataStore.edit {
                it[PreferencesKeys.BibleVerse.currentItem] = value
            }
        }
    }

    object Display {
        fun getFontSize(context: Context) = getFloat(context, PreferencesKeys.Display.fontSize, 16F)
        fun isDarkMode(context: Context): Boolean {
            val defValue = when(context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                AppCompatDelegate.MODE_NIGHT_YES, Configuration.UI_MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_NO -> false
                AppCompatDelegate.MODE_NIGHT_UNSPECIFIED, Configuration.UI_MODE_NIGHT_UNDEFINED -> true
                else -> true
            }

            return getBoolean(context, PreferencesKeys.Display.isDarkMode, defValue)
        }

        suspend fun putFontSize(context: Context, value: Float) {
            context.dataStore.edit {
                it[PreferencesKeys.Display.fontSize] = value
            }
        }

        suspend fun putDarkMode(context: Context, value: Boolean) {
            context.dataStore.edit {
                it[PreferencesKeys.Display.isDarkMode] = value
            }
        }
    }

    object LockScreen {
        fun getDisplayAfterUnlocking(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.displayAfterUnlocking, false)
        fun getShowOnLockScreen(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.showOnLockScreen, true)
        fun getUnlockWithBackKey(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.unlockWithBackKey, false)

        suspend fun putDisplayAfterUnlocking(context: Context, value: Boolean) {
            context.dataStore.edit {
                it[PreferencesKeys.LockScreen.displayAfterUnlocking] = value
            }
        }

        suspend fun putShowOnLockScreen(context: Context, value: Boolean) {
            context.dataStore.edit {
                it[PreferencesKeys.LockScreen.showOnLockScreen] = value
            }
        }
        suspend fun putUnlockWithBackKey(context: Context, value: Boolean) {
            context.dataStore.edit {
                it[PreferencesKeys.LockScreen.unlockWithBackKey] = value
            }
        }
    }
}
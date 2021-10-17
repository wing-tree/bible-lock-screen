package com.duke.orca.android.kotlin.biblelockscreen.datastore

import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "${Application.PACKAGE_NAME}.settings")

object DataStore {
    private fun getBoolean(context: Context, key: Preferences.Key<Boolean>, defValue: Boolean): Boolean = runBlocking {
        context.dataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    private fun getInt(context: Context, key: Preferences.Key<Int>, defValue: Int): Int = runBlocking {
        context.dataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    private fun getFloat(context: Context, key: Preferences.Key<Float>, defValue: Float): Float = runBlocking {
        context.dataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    private fun getLong(context: Context, key: Preferences.Key<Long>, defValue: Long): Long = runBlocking {
        context.dataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    suspend fun putBoolean(context: Context, key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit {
            it[key] = value
        }
    }

    suspend fun putInt(context: Context, key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit {
            it[key] = value
        }
    }

    suspend fun putFloat(context: Context, key: Preferences.Key<Float>, value: Float) {
        context.dataStore.edit {
            it[key] = value
        }
    }

    suspend fun putLong(context: Context, key: Preferences.Key<Long>, value: Long) {
        context.dataStore.edit {
            it[key] = value
        }
    }

    fun isFirstTime(context: Context): Boolean = getBoolean(context, PreferencesKeys.isFirstTime, true)

    fun putFirstTime(context: Context, value: Boolean) = runBlocking {
        putBoolean(context, PreferencesKeys.isFirstTime, value)
    }

    fun getBegin(context: Context): Long = getLong(context, PreferencesKeys.begin, System.currentTimeMillis())

    fun putBegin(context: Context, value: Long) = runBlocking {
        putLong(context, PreferencesKeys.begin, value)
    }

    object BibleChapter {
        fun getCurrentItem(context: Context) = getInt(context, PreferencesKeys.BibleChapter.currentItem, 0)

        fun putCurrentItem(context: Context, value: Int) = runBlocking {
            putInt(context, PreferencesKeys.BibleChapter.currentItem, value)
        }
    }

    object BibleVerse {
        fun getCurrentItem(context: Context) = getInt(context, PreferencesKeys.BibleVerse.currentItem, 0)

        fun putCurrentItem(context: Context, value: Int) = runBlocking {
            putInt(context, PreferencesKeys.BibleVerse.currentItem, value)
        }
    }

    object Display {
        fun isDarkMode(context: Context): Boolean {
            val defValue = when(context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                AppCompatDelegate.MODE_NIGHT_YES, Configuration.UI_MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_NO -> false
                AppCompatDelegate.MODE_NIGHT_UNSPECIFIED, Configuration.UI_MODE_NIGHT_UNDEFINED -> true
                else -> true
            }

            return getBoolean(context, PreferencesKeys.Display.isDarkMode, defValue)
        }

        fun putDarkMode(context: Context, value: Boolean) = runBlocking {
            putBoolean(context, PreferencesKeys.Display.isDarkMode, value)
        }
    }

    object Font {
        const val DEFAULT_FONT_SIZE = 16.0F

        object TextAlignment {
            const val CENTER = Gravity.CENTER
            const val LEFT = Gravity.LEFT
            const val RIGHT = Gravity.RIGHT
        }

        fun getFontSize(context: Context) = getFloat(context, PreferencesKeys.Font.fontSize, DEFAULT_FONT_SIZE)
        fun getBold(context: Context) = getBoolean(context, PreferencesKeys.Font.bold, false)
        fun getTextAlignment(context: Context) = getInt(context, PreferencesKeys.Font.textAlignment, TextAlignment.LEFT)

        suspend fun putFontSize(context: Context, value: Float) {
            putFloat(context, PreferencesKeys.Font.fontSize, value)
        }

        suspend fun putBold(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.Font.bold, value)
        }

        suspend fun putTextAlignment(context: Context, value: Int) {
            putInt(context, PreferencesKeys.Font.textAlignment, value)
        }
    }

    object LockScreen {
        fun getDisplayAfterUnlocking(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.displayAfterUnlocking, false)
        fun getShowOnLockScreen(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.showOnLockScreen, true)
        fun getUnlockWithBackKey(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.unlockWithBackKey, false)

        suspend fun putDisplayAfterUnlocking(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.LockScreen.displayAfterUnlocking, value)
        }

        suspend fun putShowOnLockScreen(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.LockScreen.showOnLockScreen, value)
        }
        suspend fun putUnlockWithBackKey(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.LockScreen.unlockWithBackKey, value)
        }
    }
}
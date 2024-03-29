package com.duke.orca.android.kotlin.biblelockscreen.datastore

import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.BLANK
import com.duke.orca.android.kotlin.biblelockscreen.datastore.serializers.RecentlyReadSerializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "${Application.PACKAGE_NAME}.settings")
val Context.recentlyReadDataStore: DataStore<RecentlyRead> by dataStore(
    fileName = "recently_read.pb",
    serializer = RecentlyReadSerializer
)

object DataStore {
    private fun getBoolean(context: Context, key: Preferences.Key<Boolean>, defValue: Boolean): Boolean = runBlocking {
        context.preferencesDataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    fun getBooleanFlow(context: Context, key: Preferences.Key<Boolean>, defValue: Boolean) =
        context.preferencesDataStore.data.map {
            it[key] ?: defValue
        }

    private fun getInt(context: Context, key: Preferences.Key<Int>, defValue: Int): Int = runBlocking {
        context.preferencesDataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    private fun getFloat(context: Context, key: Preferences.Key<Float>, defValue: Float): Float = runBlocking {
        context.preferencesDataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    private fun getLong(context: Context, key: Preferences.Key<Long>, defValue: Long): Long = runBlocking {
        context.preferencesDataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    private fun getString(context: Context, key: Preferences.Key<String>, defValue: String): String = runBlocking {
        context.preferencesDataStore.data.map {
            it[key] ?: defValue
        }.first()
    }

    suspend fun putBoolean(context: Context, key: Preferences.Key<Boolean>, value: Boolean) {
        context.preferencesDataStore.edit {
            it[key] = value
        }
    }

    suspend fun putInt(context: Context, key: Preferences.Key<Int>, value: Int) {
        context.preferencesDataStore.edit {
            it[key] = value
        }
    }

    suspend fun putFloat(context: Context, key: Preferences.Key<Float>, value: Float) {
        context.preferencesDataStore.edit {
            it[key] = value
        }
    }

    suspend fun putLong(context: Context, key: Preferences.Key<Long>, value: Long) {
        context.preferencesDataStore.edit {
            it[key] = value
        }
    }

    suspend fun putString(context: Context, key: Preferences.Key<String>, value: String) {
        context.preferencesDataStore.edit {
            it[key] = value
        }
    }

    fun isFirstTime(context: Context): Boolean = getBoolean(context, PreferencesKeys.isFirstTime, true)

    fun putFirstTime(context: Context, value: Boolean) = runBlocking {
        putBoolean(context, PreferencesKeys.isFirstTime, value)
    }

    object Verse {
        fun getCurrentVerse(context: Context) = getInt(context, PreferencesKeys.Verse.currentVerse, 0)

        fun putCurrentVerse(context: Context, value: Int) = runBlocking {
            putInt(context, PreferencesKeys.Verse.currentVerse, value)
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
        const val DEFAULT_SIZE = 16.0f

        object TextAlignment {
            const val CENTER = Gravity.CENTER
            const val LEFT = Gravity.LEFT
            const val RIGHT = Gravity.RIGHT
        }

        fun getSize(context: Context) = getFloat(context, PreferencesKeys.Font.size, DEFAULT_SIZE)
        fun getBold(context: Context) = getBoolean(context, PreferencesKeys.Font.bold, false)
        fun getTextAlignment(context: Context) = getInt(context, PreferencesKeys.Font.textAlignment, TextAlignment.LEFT)

        suspend fun putSize(context: Context, value: Float) {
            putFloat(context, PreferencesKeys.Font.size, value)
        }

        suspend fun putBold(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.Font.bold, value)
        }

        suspend fun putTextAlignment(context: Context, value: Int) {
            putInt(context, PreferencesKeys.Font.textAlignment, value)
        }

        object Bible {
            fun getSize(context: Context) = getFloat(context, PreferencesKeys.Font.Bible.size, DEFAULT_SIZE)
            fun getTextAlignment(context: Context) = getInt(context, PreferencesKeys.Font.Bible.textAlignment, TextAlignment.LEFT)

            suspend fun putSize(context: Context, value: Float) {
                putFloat(context, PreferencesKeys.Font.Bible.size, value)
            }

            suspend fun putTextAlignment(context: Context, value: Int) {
                putInt(context, PreferencesKeys.Font.Bible.textAlignment, value)
            }
        }
    }

    object LockScreen {
        fun getDisplayAfterUnlocking(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.displayAfterUnlocking, false)
        fun getShowOnLockScreen(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.showOnLockScreen, true)
        fun getUnlockWithBackKey(context: Context) = getBoolean(context, PreferencesKeys.LockScreen.unlockWithBackKey, true)
//        fun isSwipeOnTouch(context: Context) = getBoolean(
//            context = context,
//            key = PreferencesKeys.LockScreen.swipeOnTouch,
//            defValue = false,
//        )
//
//        fun swipeOnTouchFlow(context: Context) = getBooleanFlow(
//            context = context,
//            key = PreferencesKeys.LockScreen.swipeOnTouch,
//            defValue = false,
//        )

        suspend fun putDisplayAfterUnlocking(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.LockScreen.displayAfterUnlocking, value)
        }

        suspend fun putShowOnLockScreen(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.LockScreen.showOnLockScreen, value)
        }

        suspend fun putUnlockWithBackKey(context: Context, value: Boolean) {
            putBoolean(context, PreferencesKeys.LockScreen.unlockWithBackKey, value)
        }

//        suspend fun putSwipeOnTouch(context: Context, value: Boolean) {
//            putBoolean(context, PreferencesKeys.LockScreen.swipeOnTouch, value)
//        }
    }

    object Translation {
        fun getFileName(context: Context): String = getString(context, PreferencesKeys.Translation.fileName, BLANK)

        fun putFileName(context: Context, value: String) = runBlocking {
            putString(context, PreferencesKeys.Translation.fileName, value)
        }

        fun getSubFileName(context: Context): String = getString(context, PreferencesKeys.Translation.subFileName, BLANK)

        fun putSubFileName(context: Context, value: String) = runBlocking {
            putString(context, PreferencesKeys.Translation.subFileName, value)
        }
    }
}
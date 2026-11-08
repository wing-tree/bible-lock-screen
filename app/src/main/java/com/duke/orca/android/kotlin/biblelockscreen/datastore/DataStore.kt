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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "${Application.PACKAGE_NAME}.settings")
val Context.recentlyReadDataStore: DataStore<RecentlyRead> by dataStore(
    fileName = "recently_read.pb",
    serializer = RecentlyReadSerializer
)

object DataStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = AtomicReference<Preferences?>(null)

    // 메인 스레드에서 읽어도 막히지 않도록 Preferences 를 메모리에 들고 있는다.
    fun init(context: Context) {
        val applicationContext = context.applicationContext

        scope.launch {
            applicationContext.preferencesDataStore.data.collect { cache.set(it) }
        }
    }

    private fun preferences(context: Context): Preferences = cache.get() ?: runBlocking {
        context.preferencesDataStore.data.first()
    }.also { cache.compareAndSet(null, it) }

    private fun <T> get(context: Context, key: Preferences.Key<T>, defValue: T): T =
        preferences(context)[key] ?: defValue

    private fun getBoolean(context: Context, key: Preferences.Key<Boolean>, defValue: Boolean) = get(context, key, defValue)

    private fun getInt(context: Context, key: Preferences.Key<Int>, defValue: Int) = get(context, key, defValue)

    private fun getFloat(context: Context, key: Preferences.Key<Float>, defValue: Float) = get(context, key, defValue)

    private fun getString(context: Context, key: Preferences.Key<String>, defValue: String) = get(context, key, defValue)

    private suspend fun <T> put(context: Context, key: Preferences.Key<T>, value: T) {
        cache.set(context.preferencesDataStore.edit { it[key] = value })
    }

    // 기다리지 않는 쓰기: 캐시를 먼저 바꿔 바로 다음 읽기에도 새 값이 보이게 한다.
    private fun <T> putAsync(context: Context, key: Preferences.Key<T>, value: T) {
        cache.set(preferences(context).toMutablePreferences().apply { this[key] = value })

        val applicationContext = context.applicationContext

        scope.launch { put(applicationContext, key, value) }
    }

    suspend fun putBoolean(context: Context, key: Preferences.Key<Boolean>, value: Boolean) = put(context, key, value)

    suspend fun putInt(context: Context, key: Preferences.Key<Int>, value: Int) = put(context, key, value)

    suspend fun putFloat(context: Context, key: Preferences.Key<Float>, value: Float) = put(context, key, value)

    suspend fun putString(context: Context, key: Preferences.Key<String>, value: String) = put(context, key, value)

    fun isFirstTime(context: Context): Boolean = getBoolean(context, PreferencesKeys.isFirstTime, true)

    fun putFirstTime(context: Context, value: Boolean) = putAsync(context, PreferencesKeys.isFirstTime, value)

    object Verse {
        fun getCurrentVerse(context: Context) = getInt(context, PreferencesKeys.Verse.currentVerse, 0)

        fun putCurrentVerse(context: Context, value: Int) = putAsync(context, PreferencesKeys.Verse.currentVerse, value)
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

        fun putDarkMode(context: Context, value: Boolean) = putAsync(context, PreferencesKeys.Display.isDarkMode, value)
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

    object Translation {
        fun getFileName(context: Context): String = getString(context, PreferencesKeys.Translation.fileName, BLANK)

        fun putFileName(context: Context, value: String) = putAsync(context, PreferencesKeys.Translation.fileName, value)

        fun getSubFileName(context: Context): String = getString(context, PreferencesKeys.Translation.subFileName, BLANK)

        fun putSubFileName(context: Context, value: String) = putAsync(context, PreferencesKeys.Translation.subFileName, value)
    }
}
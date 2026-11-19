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

    // 디스크 쓰기는 부른 순서대로 하나씩 한다.
    private val writer = Dispatchers.IO.limitedParallelism(1)

    // 메인 스레드에서 읽어도 막히지 않도록 Preferences 를 메모리에 들고 있는다.
    // 쓰기는 모두 이 객체를 거치므로 처음 한 번만 읽고, 그 뒤로는 쓸 때마다 캐시에 바로 반영한다.
    // (디스크 변경을 받아 캐시를 통째로 덮으면, 먼저 끝난 쓰기의 옛 스냅숏이 방금 쓴 값을 지운다.)
    fun init(context: Context) {
        val applicationContext = context.applicationContext

        scope.launch { preferences(applicationContext) }
    }

    private fun preferences(context: Context): Preferences = cache.get() ?: run {
        cache.compareAndSet(null, runBlocking { context.preferencesDataStore.data.first() })
        cache.get()!!
    }

    private fun <T> remember(context: Context, key: Preferences.Key<T>, value: T) {
        preferences(context)
        cache.updateAndGet { it!!.toMutablePreferences().apply { this[key] = value } }
    }

    private fun <T> get(context: Context, key: Preferences.Key<T>, defValue: T): T =
        preferences(context)[key] ?: defValue

    private fun <T : Any> get(context: Context, key: Preferences.Key<T>, defValue: Nothing?): T? =
        preferences(context)[key]

    private fun getBoolean(context: Context, key: Preferences.Key<Boolean>, defValue: Boolean) = get(context, key, defValue)

    private fun getInt(context: Context, key: Preferences.Key<Int>, defValue: Int) = get(context, key, defValue)

    private fun getFloat(context: Context, key: Preferences.Key<Float>, defValue: Float) = get(context, key, defValue)

    private fun getString(context: Context, key: Preferences.Key<String>, defValue: String) = get(context, key, defValue)

    private suspend fun <T> put(context: Context, key: Preferences.Key<T>, value: T) {
        remember(context, key, value)
        context.preferencesDataStore.edit { it[key] = value }
    }

    // 기다리지 않는 쓰기: 캐시를 먼저 바꿔 바로 다음 읽기에도 새 값이 보이게 한다.
    private fun <T> putAsync(context: Context, key: Preferences.Key<T>, value: T) {
        remember(context, key, value)

        val applicationContext = context.applicationContext

        scope.launch(writer) { applicationContext.preferencesDataStore.edit { it[key] = value } }
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
        // AppCompatDelegate.MODE_NIGHT_* 값을 그대로 저장한다. 고른 적이 없으면 시스템 설정을 따른다.
        // 옛 켜기·끄기를 직접 바꾼 적이 있으면 그 값을 이어 쓴다.
        fun getNightMode(context: Context): Int {
            get(context, PreferencesKeys.Display.nightMode, null)?.let { return it }

            return when (get(context, PreferencesKeys.Display.isDarkMode, null)) {
                true -> AppCompatDelegate.MODE_NIGHT_YES
                false -> AppCompatDelegate.MODE_NIGHT_NO
                null -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }

        // 메인 스레드에서 부른다. 바꾸면 AppCompat 이 열린 화면을 알아서 다시 만든다.
        fun putNightMode(context: Context, value: Int) {
            putAsync(context, PreferencesKeys.Display.nightMode, value)
            AppCompatDelegate.setDefaultNightMode(value)
        }

        // 지금 화면이 실제로 어두운지(시스템 설정 따르기일 때도 맞게 나온다).
        fun isDarkMode(context: Context): Boolean =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
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
        fun getVerseOrder(context: Context) = getInt(context, PreferencesKeys.LockScreen.verseOrder, VerseOrder.SEQUENTIAL)

        // 잠금화면을 띄울 때 어느 구절에서 시작할지.
        object VerseOrder {
            const val SEQUENTIAL = 0 // 마지막으로 본 구절부터
            const val DAILY = 1 // 오늘의 말씀(엄선 구절 하루 하나)부터
            const val FAVORITES = 2 // 즐겨찾기한 구절만
            const val RANDOM = 3 // 열 때마다 아무 구절에서(넘기면 앞뒤 구절)
        }

        suspend fun putVerseOrder(context: Context, value: Int) {
            putInt(context, PreferencesKeys.LockScreen.verseOrder, value)
        }

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

    object Ad {
        // 잠금화면을 연 횟수(앱이 다시 떠도 이어서 센다).
        fun getOpens(context: Context) = getInt(context, PreferencesKeys.Ad.opens, 0)

        fun putOpens(context: Context, value: Int) = putAsync(context, PreferencesKeys.Ad.opens, value)

        // 광고 제거를 샀는지(Play 에 닿지 않아도 이 값으로 광고를 끈다).
        fun isAdFree(context: Context) = getBoolean(context, PreferencesKeys.Ad.isAdFree, false)

        fun putAdFree(context: Context, value: Boolean) = putAsync(context, PreferencesKeys.Ad.isAdFree, value)
    }

    object Notice {
        const val DEFAULT_HOUR = 8

        // 오늘의 말씀 알림. 처음엔 켜져 있다.
        fun isDailyVerseOn(context: Context) = getBoolean(context, PreferencesKeys.Notice.isDailyVerseOn, true)

        fun putDailyVerseOn(context: Context, value: Boolean) = putAsync(context, PreferencesKeys.Notice.isDailyVerseOn, value)

        fun getDailyVerseHour(context: Context) = getInt(context, PreferencesKeys.Notice.dailyVerseHour, DEFAULT_HOUR)

        fun putDailyVerseHour(context: Context, value: Int) = putAsync(context, PreferencesKeys.Notice.dailyVerseHour, value)
    }

    object Widget {
        // 위젯에서 오늘의 말씀으로부터 몇 구절 옮겨 갔는지. 그날(epoch day)이 지나면 0 으로 본다.
        fun getOffset(context: Context, today: Long): Int =
            if (get(context, PreferencesKeys.Widget.day, 0L) == today) getInt(context, PreferencesKeys.Widget.offset, 0) else 0

        fun putOffset(context: Context, today: Long, value: Int) {
            putAsync(context, PreferencesKeys.Widget.day, today)
            putAsync(context, PreferencesKeys.Widget.offset, value)
        }
    }

    object Record {
        // 옛 번역본 DB 들의 표시를 기록 저장소로 한 번 모았는지.
        fun isMerged(context: Context) = getBoolean(context, PreferencesKeys.Record.isMerged, false)

        fun putMerged(context: Context, value: Boolean) = putAsync(context, PreferencesKeys.Record.isMerged, value)

        // 북마크를 즐겨찾기로 한 번 옮겼는지.
        fun isBookmarkMoved(context: Context) = getBoolean(context, PreferencesKeys.Record.isBookmarkMoved, false)

        fun putBookmarkMoved(context: Context, value: Boolean) = putAsync(context, PreferencesKeys.Record.isBookmarkMoved, value)
    }

    object Translation {
        fun getFileName(context: Context): String = getString(context, PreferencesKeys.Translation.fileName, BLANK)

        fun putFileName(context: Context, value: String) = putAsync(context, PreferencesKeys.Translation.fileName, value)

        fun getSubFileName(context: Context): String = getString(context, PreferencesKeys.Translation.subFileName, BLANK)

        fun putSubFileName(context: Context, value: String) = putAsync(context, PreferencesKeys.Translation.subFileName, value)
    }
}
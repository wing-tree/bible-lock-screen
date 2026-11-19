package com.duke.orca.android.kotlin.biblelockscreen.ad

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.BuildConfig
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import timber.log.Timber
import java.time.LocalDate

// 잠금화면 구절 카드에 가끔 섞는 네이티브 광고. 하나를 미리 받아 두었다가, 넘긴 횟수가 차면 다음 카드에 붙인다.
object NativeAds {
    // 이만큼 넘길 때마다 한 번. 잠금화면은 몇 장 보고 바로 푸는 일이 많아 너무 길면 거의 안 나온다.
    const val INTERVAL = 7

    // 하루에 보이는 최대 횟수. 성경 앱이라 너무 잦지 않게 막는다.
    private const val DAILY_LIMIT = 10

    private const val RETRY_DELAY = 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private val impressed = mutableSetOf<LevelPlayNativeAd>()

    private lateinit var context: Context

    var isInitialized = false
        private set

    private var loading = false
    private var ready: LevelPlayNativeAd? = null
    private var swipes = 0
        set(value) {
            field = value
            DataStore.Ad.putSwipes(context, value)
        }

    private val listener = object : LevelPlayNativeAdListener {
        override fun onAdLoaded(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {
            loading = false
            ready = nativeAd
        }

        override fun onAdLoadFailed(nativeAd: LevelPlayNativeAd?, error: IronSourceError?) {
            Timber.w("native ad load failed: $error")
            loading = false
            nativeAd?.destroyAd()
            handler.postDelayed(::load, RETRY_DELAY)
        }

        override fun onAdImpression(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {
            nativeAd?.let { impressed.add(it) }
            DataStore.Ad.putShown(context, LocalDate.now().toEpochDay(), shownToday().inc())
        }

        override fun onAdClicked(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) = Unit
    }

    fun init(context: Context) {
        // 앱 키가 없으면 광고를 아예 붙이지 않는다.
        if (LevelPlayKeys.APP_KEY.isBlank()) return

        this.context = context.applicationContext
        swipes = DataStore.Ad.getSwipes(context)

        if (BuildConfig.DEBUG) {
            LevelPlay.setAdaptersDebug(true)
            LevelPlay.setMetaData("is_test_suite", "enable")
        }

        LevelPlay.init(
            context,
            LevelPlayInitRequest.Builder(LevelPlayKeys.APP_KEY).build(),
            object : LevelPlayInitListener {
                override fun onInitSuccess(configuration: LevelPlayConfiguration) {
                    if (BuildConfig.DEBUG) LevelPlay.validateIntegration(context)
                    isInitialized = true
                    load()
                }

                override fun onInitFailed(error: LevelPlayInitError) {
                    Timber.w("LevelPlay init failed: $error")
                }
            }
        )
    }

    private fun load() {
        if (isInitialized.not() || loading || ready != null) return

        loading = true
        LevelPlayNativeAd.Builder()
            .withListener(listener)
            .build()
            .loadAd()
    }

    fun onSwiped() {
        if (isInitialized.not()) return

        swipes++
    }

    // 넘긴 횟수가 찼고, 오늘 상한이 남았고, 받아 둔 광고가 있을 때만 꺼내 준다.
    fun take(): LevelPlayNativeAd? {
        if (swipes < INTERVAL) return null

        if (shownToday() >= DAILY_LIMIT) return null

        val nativeAd = ready ?: return null

        ready = null
        swipes = 0

        return nativeAd
    }

    // 오늘 실제로 보인(노출된) 횟수.
    private fun shownToday(): Int =
        if (DataStore.Ad.getShownDay(context) == LocalDate.now().toEpochDay()) DataStore.Ad.getShownCount(context) else 0

    // 카드가 사라질 때 돌려받는다. 보인 광고는 버리고 새로 받고, 못 보인 광고는 다음 차례에 다시 쓴다.
    fun giveBack(nativeAd: LevelPlayNativeAd) {
        if (impressed.remove(nativeAd) || ready != null) {
            nativeAd.destroyAd()
        } else {
            ready = nativeAd
            swipes = INTERVAL
        }

        load()
    }
}

object LevelPlayKeys {
    // 비어 있으면 광고가 나오지 않는다.
    const val APP_KEY = "295fa9cad"

    // 읽기 화면 아래 배너 단위. 비어 있으면 배너를 붙이지 않는다.
    const val BANNER_AD_UNIT_ID = ""

    // 네이티브 광고 단위. SDK 9.7 네이티브 API 는 단위 ID 를 받지 않아(앱 키로 대시보드 단위가 잡힌다) 받아만 둔다.
    const val NATIVE_AD_UNIT_ID = "phckhlax6jdk9td4"
}

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
import java.util.concurrent.TimeUnit

// 잠금화면 구절 카드에 섞는 네이티브 광고. 하나를 받아 두고, 꺼내 가면 바로 다음 것을 받는다.
// 잠금화면을 OPEN_INTERVAL 번 열 때마다 첫 카드에, 한 판 안에서는 연 횟수와 상관없이 SWIPE_INTERVAL 장마다 붙인다.
// 새로 설치하고 GRACE_PERIOD 동안은 네이티브·배너 모두 붙이지 않는다(처음 며칠의 이탈을 줄이려고).
object NativeAds {
    private const val OPEN_INTERVAL = 3
    private const val SWIPE_INTERVAL = 7

    private const val RETRY_DELAY = 60_000L
    private val GRACE_PERIOD = TimeUnit.DAYS.toMillis(3)

    private val handler = Handler(Looper.getMainLooper())
    private val impressed = mutableSetOf<LevelPlayNativeAd>()

    private lateinit var context: Context

    var isInitialized = false
        private set

    private var installedAt = 0L

    // 설치한 지 GRACE_PERIOD 가 안 지났는지.
    val isInGracePeriod: Boolean
        get() = System.currentTimeMillis() - installedAt < GRACE_PERIOD

    // 앱 키가 있어 광고를 쓰는지. SDK 초기화 전에도 연 횟수는 센다.
    private var enabled = false

    private var loading = false
    private var ready: LevelPlayNativeAd? = null

    // 잠금화면을 연 횟수(앱이 다시 떠도 이어서 센다).
    private var opens = 0
        set(value) {
            field = value
            DataStore.Ad.putOpens(context, value)
        }

    // 이번 판의 첫 카드(또는 마지막 광고 카드)에서 몇 장 넘어왔는지.
    private var swipes = 0

    private val listener = object : LevelPlayNativeAdListener {
        override fun onAdLoaded(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {
            loading = false

            // 받는 사이에 못 보인 광고를 돌려받았으면 그걸 먼저 쓰고 새 것은 버린다.
            if (ready == null) ready = nativeAd else nativeAd?.destroyAd()
        }

        override fun onAdLoadFailed(nativeAd: LevelPlayNativeAd?, error: IronSourceError?) {
            Timber.w("native ad load failed: $error")
            loading = false
            nativeAd?.destroyAd()
            handler.postDelayed(::load, RETRY_DELAY)
        }

        override fun onAdImpression(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {
            nativeAd?.let { impressed.add(it) }
        }

        override fun onAdClicked(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) = Unit
    }

    fun init(context: Context) {
        // 앱 키가 없으면 광고를 아예 붙이지 않는다.
        if (LevelPlayKeys.APP_KEY.isBlank()) return

        this.context = context.applicationContext
        opens = DataStore.Ad.getOpens(context)
        installedAt = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: Exception) {
            0L
        }
        enabled = true

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

    // 잠금화면이 새로 열렸다.
    fun onOpened() {
        if (enabled.not()) return

        opens++
        swipes = 0
    }

    fun onSwiped() {
        swipes++
    }

    // 첫 카드에 붙일 차례면 꺼내 준다.
    fun takeForOpen(): LevelPlayNativeAd? {
        if (isInGracePeriod || opens < OPEN_INTERVAL) return null

        return take()?.also { opens = 0 }
    }

    // 넘긴 방향의 다음 카드가 SWIPE_INTERVAL 장째면 꺼내 준다.
    // 다음에 넘기면 광고 카드에 닿아 0 이 되도록 -1 로 둔다.
    fun takeForSwipe(): LevelPlayNativeAd? {
        if (isInGracePeriod || swipes < SWIPE_INTERVAL.dec()) return null

        return take()?.also { swipes = -1 }
    }

    private fun take(): LevelPlayNativeAd? {
        val nativeAd = ready ?: return null

        ready = null
        load()

        return nativeAd
    }

    // 카드에 자리가 없어 못 붙였다. 차례를 되돌려 다음 열기(또는 다음 카드)에 다시 쓴다.
    fun putBackForOpen(nativeAd: LevelPlayNativeAd) {
        opens = OPEN_INTERVAL
        giveBack(nativeAd)
    }

    fun putBackForSwipe(nativeAd: LevelPlayNativeAd) {
        swipes = SWIPE_INTERVAL.dec()
        giveBack(nativeAd)
    }

    // 카드가 사라질 때 돌려받는다. 보인 광고는 버리고, 못 보인 광고는 다음 차례에 다시 쓴다.
    fun giveBack(nativeAd: LevelPlayNativeAd) {
        if (impressed.remove(nativeAd) || ready != null) {
            nativeAd.destroyAd()
        } else {
            ready = nativeAd
        }

        load()
    }
}

object LevelPlayKeys {
    // 비어 있으면 광고가 나오지 않는다.
    const val APP_KEY = "295fa9cad"

    // 읽기 화면 아래 배너 단위. 비어 있으면 배너를 붙이지 않는다.
    const val BANNER_AD_UNIT_ID = "4utu7a1opm76mrkc"

    // 네이티브 광고 단위. SDK 9.7 네이티브 API 는 단위 ID 를 받지 않아(앱 키로 대시보드 단위가 잡힌다) 받아만 둔다.
    const val NATIVE_AD_UNIT_ID = "phckhlax6jdk9td4"
}

package com.duke.orca.android.kotlin.biblelockscreen.ad

import android.content.Context
import android.os.Handler
import android.os.Looper
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

// 잠금화면 구절 카드에 가끔 섞는 네이티브 광고. 하나를 미리 받아 두었다가, 넘긴 횟수가 차면 다음 카드에 붙인다.
object NativeAds {
    // 이만큼 넘길 때마다 한 번. 성경 앱이라 최대한 드물게 둔다.
    const val INTERVAL = 20

    private const val RETRY_DELAY = 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private val impressed = mutableSetOf<LevelPlayNativeAd>()

    private var initialized = false
    private var loading = false
    private var ready: LevelPlayNativeAd? = null
    private var swipes = 0

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
        }

        override fun onAdClicked(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) = Unit
    }

    fun init(context: Context) {
        // 앱 키가 없으면 광고를 아예 붙이지 않는다.
        if (LevelPlayKeys.APP_KEY.isBlank()) return

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
                    initialized = true
                    load()
                }

                override fun onInitFailed(error: LevelPlayInitError) {
                    Timber.w("LevelPlay init failed: $error")
                }
            }
        )
    }

    private fun load() {
        if (initialized.not() || loading || ready != null) return

        loading = true
        LevelPlayNativeAd.Builder()
            .withListener(listener)
            .build()
            .loadAd()
    }

    fun onSwiped() {
        swipes++
    }

    // 넘긴 횟수가 찼고 받아 둔 광고가 있을 때만 꺼내 준다.
    fun take(): LevelPlayNativeAd? {
        if (swipes < INTERVAL) return null

        val nativeAd = ready ?: return null

        ready = null
        swipes = 0

        return nativeAd
    }

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
    // LevelPlay 대시보드에서 이 앱을 등록한 뒤 채운다. 비어 있으면 광고가 나오지 않는다.
    const val APP_KEY = ""
}

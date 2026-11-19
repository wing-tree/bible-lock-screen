package com.duke.orca.android.kotlin.biblelockscreen.ad

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayAdSize
import com.unity3d.mediation.banner.LevelPlayBannerAdView
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener
import timber.log.Timber

// 읽기 화면 아래 배너. 단위 ID 가 비었거나 LevelPlay 가 아직 안 켜졌거나 광고를 쉬는 동안(NativeAds.isHeld)엔 붙이지 않는다.
// 자리(container)는 광고를 받은 뒤에야 보이게 한다.
object BannerAds {
    fun attach(container: ViewGroup): LevelPlayBannerAdView? {
        if (LevelPlayKeys.BANNER_AD_UNIT_ID.isBlank() || NativeAds.isInitialized.not() || NativeAds.isHeld) return null

        val context = container.context
        val adSize = LevelPlayAdSize.createAdaptiveAdSize(context) ?: LevelPlayAdSize.BANNER
        val config = LevelPlayBannerAdView.Config.Builder()
            .setAdSize(adSize)
            .build()

        return LevelPlayBannerAdView(context, LevelPlayKeys.BANNER_AD_UNIT_ID, config).apply {
            setBannerListener(object : LevelPlayBannerAdViewListener {
                override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                    container.isVisible = true
                }

                override fun onAdLoadFailed(error: LevelPlayAdError) {
                    Timber.w("banner ad load failed: $error")
                }
            })

            container.addView(this)
            loadAd()
        }
    }
}

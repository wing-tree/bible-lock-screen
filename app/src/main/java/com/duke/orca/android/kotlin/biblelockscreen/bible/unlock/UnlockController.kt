package com.duke.orca.android.kotlin.biblelockscreen.bible.unlock

import android.view.MotionEvent
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.toPx
import com.duke.orca.android.kotlin.biblelockscreen.application.hideRipple
import com.duke.orca.android.kotlin.biblelockscreen.application.scale
import com.duke.orca.android.kotlin.biblelockscreen.application.showRipple
import com.duke.orca.android.kotlin.biblelockscreen.application.translateX
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentVersePagerBinding
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class UnlockController(
    private val viewBinding: FragmentVersePagerBinding,
    private val callback: Callback
) {
    interface Callback {
        fun onOutOfRanged()
    }

    private val isOutOfRanged = AtomicBoolean(false)
    private val range = 600.0f

    private var x = 0.0f
    private var y = 0.0f

    fun init() {
        viewBinding.constraintLayoutUnlock.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    disableUserInput()
                    x = event.x
                    y = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    viewBinding.frameLayoutUnlock.showRipple()

                    val distance = sqrt((x - event.x).pow(2) + (y - event.y).pow(2))
                    var scale = abs(range - distance * 0.4f) / range

                    when {
                        scale >= 1.0f -> scale = 1.0f
                        scale < 0.8f -> scale = 0.8f
                    }

                    val alpha = (scale - 0.8f) * 5.0f

                    setAlpha(alpha)
                    setScale(scale)

                    viewBinding.viewPreviousFake.translationX = ((1.0f - alpha) * -8.0f).toPx
                    viewBinding.viewNextFake.translationX = ((1.0f - alpha) * 8.0f).toPx

                    isOutOfRanged.set(distance * 1.25f > range * 0.75f)
                }
                MotionEvent.ACTION_UP -> {
                    if (isOutOfRanged.get()) {
                        callback.onOutOfRanged()
                    } else {
                        restore()
                    }
                }
            }

            true
        }
    }

    private fun setAlpha(alpha: Float) {
        viewBinding.imageViewUnlock.alpha = alpha
        viewBinding.linearLayout.alpha = alpha
        viewBinding.viewPreviousFake.alpha = alpha
        viewBinding.viewNextFake.alpha = alpha
    }

    private fun setScale(scale: Float) {
        viewBinding.linearLayout.scaleX = scale
        viewBinding.linearLayout.scaleY = scale
        viewBinding.imageViewUnlock.scaleX = scale
        viewBinding.imageViewUnlock.scaleY = scale
    }

    fun restore() {
        viewBinding.frameLayoutUnlock.hideRipple()
        viewBinding.imageViewUnlock.scale(1.0f, duration = Duration.MEDIUM)
        viewBinding.linearLayout.scale(1.0f, duration =  Duration.MEDIUM)
        viewBinding.viewPreviousFake.translateX(0.0f, duration = Duration.MEDIUM)
        viewBinding.viewNextFake.translateX(0.0f, duration = Duration.MEDIUM) {
            enableUserInput()
        }
    }

    private fun disableUserInput() {
        viewBinding.viewPager2.isUserInputEnabled = false
    }

    private fun enableUserInput() {
        viewBinding.viewPager2.isUserInputEnabled = true
    }
}
package com.duke.orca.android.kotlin.biblelockscreen.base.views

import android.app.KeyguardManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore

open class BaseLockScreenActivity : AppCompatActivity() {
    protected val activity: BaseLockScreenActivity
        get() = this

    private var keyguardManager: KeyguardManager? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (DataStore.LockScreen.getShowOnLockScreen(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)

                // https://stackoverflow.com/questions/60477120/keyguardmanager-memory-leak
                keyguardManager = getSystemService(KeyguardManager::class.java)

                keyguardManager?.let {
                    if (it.isKeyguardLocked) {
                        it.requestDismissKeyguard(activity, null)
                    }
                }

                keyguardManager = null
            } else {
                @Suppress("DEPRECATION")
                val flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

                window.addFlags(flags)
            }
        }
    }

    // API 35 부터는 엣지 투 엣지가 강제되므로 시스템 바만큼 화면 안쪽으로 밀고,
    // 비워진 자리에 테마의 상태 바·내비게이션 바 색을 칠한다.
    @CallSuper
    override fun onContentChanged() {
        super.onContentChanged()

        val barColors = theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.statusBarColor, android.R.attr.navigationBarColor)
        ).run {
            try {
                getColor(0, 0) to getColor(1, 0)
            } finally {
                recycle()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            view.background = LayerDrawable(
                arrayOf(ColorDrawable(barColors.first), ColorDrawable(barColors.second))
            ).apply {
                setLayerGravity(0, Gravity.TOP or Gravity.FILL_HORIZONTAL)
                setLayerHeight(0, insets.top)
                setLayerGravity(1, Gravity.BOTTOM or Gravity.FILL_HORIZONTAL)
                setLayerHeight(1, insets.bottom)
            }

            windowInsets
        }
    }
}

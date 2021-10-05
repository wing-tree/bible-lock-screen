package com.duke.orca.android.kotlin.biblelockscreen.main.view

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.Duration
import com.duke.orca.android.kotlin.biblelockscreen.application.SystemUiColorAction
import com.duke.orca.android.kotlin.biblelockscreen.base.views.LockScreenActivity
import com.duke.orca.android.kotlin.biblelockscreen.bibleverses.views.BibleVersePagerFragment
import com.duke.orca.android.kotlin.biblelockscreen.lockscreen.service.LockScreenService
import com.duke.orca.android.kotlin.biblelockscreen.main.viewmodel.MainViewModel
import com.duke.orca.android.kotlin.biblelockscreen.permission.PermissionChecker
import com.duke.orca.android.kotlin.biblelockscreen.permission.view.PermissionRationaleDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : LockScreenActivity(), PermissionRationaleDialogFragment.OnPermissionAllowClickListener {
    private val viewModel by viewModels<MainViewModel>()

    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (PermissionRationaleDialogFragment.permissionsGranted(this).not()) {
            PermissionRationaleDialogFragment().also {
                it.show(supportFragmentManager, it.tag)
            }
        }

        startService()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, BibleVersePagerFragment())
            .commit()

        viewModel.systemUiColorChanged.observe(this, {
            when(it) {
                SystemUiColorAction.SET -> setSystemUiColor()
                SystemUiColorAction.REVERT -> revertSystemUiColor()
                else -> {
                }
            }
        })
    }

    private fun setSystemUiColor() {
        val startValue = ContextCompat.getColor(this, R.color.system_ui)
        val endValue = ContextCompat.getColor(this, R.color.system_ui2)

        if (window.statusBarColor == endValue) return

        with(ValueAnimator.ofObject(ArgbEvaluator(), startValue, endValue)) {
            duration = Duration.SHORT

            this.addUpdateListener {
                val animatedValue = it.animatedValue as Int

                with(window) {
                    statusBarColor = animatedValue
                    navigationBarColor = animatedValue
                }
            }

            start()
        }
    }

    private fun revertSystemUiColor() {
        val startValue = ContextCompat.getColor(this, R.color.system_ui2)
        val endValue = ContextCompat.getColor(this, R.color.system_ui)

        if (window.statusBarColor == endValue) return

        with(ValueAnimator.ofObject(ArgbEvaluator(), startValue, endValue)) {
            duration = Duration.SHORT

            this.addUpdateListener {
                val animatedValue = it.animatedValue as Int

                with(window) {
                    statusBarColor = animatedValue
                    navigationBarColor = animatedValue
                }
            }

            start()
        }
    }

    override fun onPermissionAllowClick() {
        checkManageOverlayPermission()
    }

    override fun onPermissionDenyClick() {
        if (PermissionChecker.hasManageOverlayPermission()) {
            startService()
        } else {
            finish()
        }
    }

    private fun startService() {
        val intent = Intent(applicationContext, LockScreenService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkManageOverlayPermission() {
        if (PermissionChecker.hasManageOverlayPermission())
            startService()
        else {
            val uri = Uri.fromParts("package", packageName, null)
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)

            handler = Handler(mainLooper)

            handler?.postDelayed(object : Runnable {
                override fun run() {
                    if (Settings.canDrawOverlays(this@MainActivity)) {
                        Intent(this@MainActivity, MainActivity::class.java).run {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            val customAnimation = ActivityOptions.makeCustomAnimation(
                                this@MainActivity,
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )

                            startActivity(this, customAnimation.toBundle())
                        }

                        handler = null
                        return
                    }

                    handler?.postDelayed(this, Duration.LONG)
                }
            }, Duration.LONG)
        }
    }
}
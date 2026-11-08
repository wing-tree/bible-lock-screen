package com.duke.orca.android.kotlin.biblelockscreen.main.view

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.EXTRA_APP_PACKAGE
import androidx.activity.result.contract.ActivityResultContracts
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseLockScreenActivity
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.extension.checkPermission
import com.duke.orca.android.kotlin.biblelockscreen.lockscreen.service.LockScreenService
import com.duke.orca.android.kotlin.biblelockscreen.permission.model.Permission
import com.duke.orca.android.kotlin.biblelockscreen.permission.view.PermissionRationaleDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseLockScreenActivity(), PermissionRationaleDialogFragment.OnPermissionAllowClickListener {
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && PermissionRationaleDialogFragment.permissionsGranted(this)) {
            startService()
        } else {
            showPermissionRationale()
        }
    }

    // 설정 화면에서 권한을 주고 돌아오면 onResume 에서 서비스를 시작한다.
    private var awaitingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            when {
                checkPermission(permission) -> checkManageOverlayPermission()
                shouldShowRequestPermissionRationale(permission) -> if (LockScreenService.isRunning.not()) {
                    showPermissionRationale()
                }
                else -> activityResultLauncher.launch(permission)
            }
        } else {
            checkManageOverlayPermission()
        }
    }

    override fun onResume() {
        super.onResume()

        if (awaitingPermission && PermissionRationaleDialogFragment.permissionsGranted(this)) {
            awaitingPermission = false
            startService()
        }
    }

    override fun onStart() {
        super.onStart()

        if (DataStore.isFirstTime(this)) {
            DataStore.putFirstTime(this, false)
        }
    }

    override fun onPermissionAllowClick(permission: Permission) {
        when (permission.permission) {
            Manifest.permission.POST_NOTIFICATIONS -> checkPostNotificationsPermission()
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION -> checkManageOverlayPermission()
        }
    }

    private fun showPermissionRationale() {
        PermissionRationaleDialogFragment().also {
            it.show(supportFragmentManager, it.tag)
        }
    }

    private fun startService() {
        LockScreenService.start(applicationContext)
    }

    private fun checkManageOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            startService()
        } else {
            val uri = Uri.fromParts("package", packageName, null)

            openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
        }
    }

    private fun checkPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            checkManageOverlayPermission()
        } else {
            openSettings(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    private fun openSettings(intent: Intent) {
        awaitingPermission = true
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

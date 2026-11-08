package com.duke.orca.android.kotlin.biblelockscreen.devicebootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.duke.orca.android.kotlin.biblelockscreen.lockscreen.service.LockScreenService

class DeviceBootReceiver : BroadcastReceiver()  {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // 다른 앱 위에 표시 권한이 없으면 백그라운드에서 잠금화면을 띄울 수 없다.
        if (Settings.canDrawOverlays(context)) {
            LockScreenService.start(context)
        }
    }
}

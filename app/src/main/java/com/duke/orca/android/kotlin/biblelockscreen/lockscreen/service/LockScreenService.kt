package com.duke.orca.android.kotlin.biblelockscreen.lockscreen.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.main.view.MainActivity

class LockScreenService : Service() {
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val showOnLockScreen = DataStore.LockScreen.getShowOnLockScreen(context)
            val displayAfterUnlocking = DataStore.LockScreen.getDisplayAfterUnlocking(context)

            if (showOnLockScreen.not()) return

            // 통화 중 근접 센서로 화면이 꺼질 때 통화 화면을 덮지 않는다.
            if (isInCall(context)) return

            val show = when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> displayAfterUnlocking.not()
                Intent.ACTION_USER_PRESENT -> displayAfterUnlocking
                else -> false
            }

            if (show) {
                // 옵션이 꺼진 채 만들어진 화면이 남아 있으면 잠금화면 뒤에 가려지므로, 늘 새로 만든다.
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(this)
                }
            }
        }
    }

    private fun isInCall(context: Context): Boolean {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return false

        return when (audioManager.mode) {
            AudioManager.MODE_RINGTONE,
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION -> true
            else -> false
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService 뒤에는 바로 startForeground 를 불러야 한다(5초 제한).
        ServiceCompat.startForeground(
            this,
            NotificationBuilder.ID,
            NotificationBuilder.create(this).build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        if (DataStore.LockScreen.getShowOnLockScreen(this).not()) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRunning = false
            private set

        fun start(context: Context) {
            if (DataStore.LockScreen.getShowOnLockScreen(context).not()) return

            ContextCompat.startForegroundService(context, Intent(context, LockScreenService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenService::class.java))
        }
    }
}

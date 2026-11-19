package com.duke.orca.android.kotlin.biblelockscreen.dailyverse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

// 오늘의 말씀 알람이 울리면 알림을 띄우고 다음 날 것을 건다.
class DailyVerseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                DailyVerseNotice.post(context)
                DailyVerseWidget.refresh(context)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                DailyVerseNotice.schedule(context)
                pendingResult.finish()
            }
        }
    }
}

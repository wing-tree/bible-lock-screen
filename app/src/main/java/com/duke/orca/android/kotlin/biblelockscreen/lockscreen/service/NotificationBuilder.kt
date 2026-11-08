package com.duke.orca.android.kotlin.biblelockscreen.lockscreen.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application

object NotificationBuilder {
    const val ID = 21522224

    private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.lockscreen.service"
    private const val OBJECT_NAME = "NotificationBuilder"
    private const val CHANNEL_ID = "$PACKAGE_NAME.$OBJECT_NAME.CHANNEL_ID"

    fun create(context: Context): NotificationCompat.Builder {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_MIN
        )

        notificationChannel.setShowBadge(false)
        notificationManager.createNotificationChannel(notificationChannel)

        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_holy_bible_100px)
            .setColor(ContextCompat.getColor(context, R.color.notification))
            .setContentTitle(context.getString(R.string.hide_notification_content_title))
            .setContentText(context.getString(R.string.hide_notification_content_text))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setShowWhen(false)
    }
}

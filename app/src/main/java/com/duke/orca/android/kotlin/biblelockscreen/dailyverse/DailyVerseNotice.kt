package com.duke.orca.android.kotlin.biblelockscreen.dailyverse

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.EXTRA_CONTENT
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.EXTRA_SIMPLE_NAME
import com.duke.orca.android.kotlin.biblelockscreen.base.views.FragmentContainerActivity
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.DailyVerses
import com.duke.orca.android.kotlin.biblelockscreen.bible.views.ChapterPagerFragment
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import kotlinx.coroutines.flow.first
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

// 하루 한 번 정한 시각에 오늘의 말씀(DailyVerses.today)을 알림으로 보낸다. 누르면 읽기 화면이 그 구절에서 열린다.
// 정확한 알람 권한 없이 10분 안쪽으로 울리게 두고, 울릴 때마다 다음 날 것을 다시 건다.
object DailyVerseNotice {
    private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.dailyverse"
    private const val CHANNEL_ID = "$PACKAGE_NAME.DailyVerseNotice.CHANNEL_ID"
    private const val ID = 21522225
    private const val WINDOW = 10 * 60 * 1000L

    // 설정이 바뀌거나 앱이 켜지거나 기기가 다시 켜질 때 부른다. 꺼져 있으면 걸어 둔 알람을 지운다.
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = alarmIntent(context)

        alarmManager.cancel(pendingIntent)

        if (DataStore.Notice.isDailyVerseOn(context).not()) return

        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, nextTrigger(DataStore.Notice.getDailyVerseHour(context)), WINDOW, pendingIntent)
    }

    // 오늘 그 시각이 아직 안 왔으면 오늘, 지났으면 내일.
    fun nextTrigger(hour: Int, now: LocalDateTime = LocalDateTime.now()): Long {
        val today = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        val next = if (today.isAfter(now)) today else today.plusDays(1)

        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    // 고른 시각을 기기 언어의 짧은 시각으로(예: 오전 8:00).
    fun format(hour: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
        }

        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
    }

    suspend fun post(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val (book, chapter, verse) = DailyVerses.today()
        val database = Database.getInstance(context)
        val item = database.verseDao().get(book, chapter, verse).first()
        val title = "${context.getString(R.string.verse_order_daily)} · ${database.bibleBookDao().get().name(book)} $chapter:$verse"

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.daily_verse_notice), NotificationManager.IMPORTANCE_DEFAULT)
        )

        val contentIntent = readerIntent(context, item.content)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_holy_bible_100px)
            .setColor(ContextCompat.getColor(context, R.color.notification))
            .setContentTitle(title)
            .setContentText(item.word)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.word))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID, notification)
    }

    // 읽기 화면을 그 구절에서 연다. 알림과 위젯이 함께 쓴다.
    fun readerIntent(context: Context, content: Content): PendingIntent {
        val intent = Intent(context, FragmentContainerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_SIMPLE_NAME, ChapterPagerFragment::class.java.simpleName)
            putExtra(EXTRA_CONTENT, content)
        }

        return PendingIntent.getActivity(
            context,
            LocalDate.now().toEpochDay().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DailyVerseReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

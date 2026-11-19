package com.duke.orca.android.kotlin.biblelockscreen.dailyverse

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.VERSE_COUNT
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.DailyVerses
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

// 홈 화면 위젯. 오늘의 말씀을 지금 번역본으로 보여 주고, 누르면 읽기 화면이 그 구절에서 열린다.
// ‹ › 로 성경 순서대로 앞뒤 구절을 보고, 날짜가 바뀌면 그날의 말씀으로 돌아간다.
// 시스템이 3시간마다 고치고, 앱이 켜질 때·오늘의 말씀 알림이 울릴 때도 고친다.
class DailyVerseWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val step = when (intent.action) {
            ACTION_PREVIOUS -> -1
            ACTION_NEXT -> 1
            else -> return super.onReceive(context, intent)
        }

        val today = LocalDate.now().toEpochDay()

        DataStore.Widget.putOffset(context, today, DataStore.Widget.getOffset(context, today) + step)
        refresh(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                update(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_PREVIOUS = "${Application.PACKAGE_NAME}.dailyverse.DailyVerseWidget.PREVIOUS"
        private const val ACTION_NEXT = "${Application.PACKAGE_NAME}.dailyverse.DailyVerseWidget.NEXT"

        fun refresh(context: Context) {
            val appWidgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, DailyVerseWidget::class.java))

            if (appWidgetIds.isEmpty()) return

            val intent = Intent(context, DailyVerseWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }

            context.sendBroadcast(intent)
        }

        private suspend fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val (book, chapter, verse) = DailyVerses.today()
            val database = Database.getInstance(context)
            val today = database.verseDao().get(book, chapter, verse).first()
            val id = (today.id + DataStore.Widget.getOffset(context, LocalDate.now().toEpochDay())).coerceIn(0, VERSE_COUNT.dec())
            val item = if (id == today.id) today else database.verseDao().get(id).first()
            val reference = "${database.bibleBookDao().get().name(item.book)} ${item.chapter}:${item.verse}"

            val views = RemoteViews(context.packageName, R.layout.widget_daily_verse).apply {
                setTextViewText(R.id.text_view_word, item.word)
                setTextViewText(R.id.text_view_reference, reference)
                setOnClickPendingIntent(R.id.root, DailyVerseNotice.readerIntent(context, item.content))
                setOnClickPendingIntent(R.id.image_view_previous, stepIntent(context, ACTION_PREVIOUS))
                setOnClickPendingIntent(R.id.image_view_next, stepIntent(context, ACTION_NEXT))
            }

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }

        private fun stepIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, DailyVerseWidget::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

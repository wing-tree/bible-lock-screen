package com.duke.orca.android.kotlin.biblelockscreen.dailyverse

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.DailyVerses
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

// 홈 화면 위젯. 오늘의 말씀을 지금 번역본으로 보여 주고, 누르면 읽기 화면이 그 구절에서 열린다.
// 시스템이 3시간마다 고치고, 앱이 켜질 때·오늘의 말씀 알림이 울릴 때도 고친다.
class DailyVerseWidget : AppWidgetProvider() {
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
            val item = database.verseDao().get(book, chapter, verse).first()
            val reference = "${database.bibleBookDao().get().name(book)} $chapter:$verse"

            val views = RemoteViews(context.packageName, R.layout.widget_daily_verse).apply {
                setTextViewText(R.id.text_view_word, item.word)
                setTextViewText(R.id.text_view_reference, reference)
                setOnClickPendingIntent(R.id.root, DailyVerseNotice.readerIntent(context, item.content))
            }

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }
}

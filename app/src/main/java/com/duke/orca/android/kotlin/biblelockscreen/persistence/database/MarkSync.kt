package com.duke.orca.android.kotlin.biblelockscreen.persistence.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Mark
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import timber.log.Timber

// 번역본 DB 를 열 때마다 기록 저장소(RecordDatabase)의 표시로 verse 표를 다시 칠한다.
// 처음 한 번은 기기에 남은 번역본 DB 들의 표시를 모아 기록 저장소에 합친다(지금 번역본이 먼저라 색이 겹치면 지금 것을 둔다).
internal class MarkSync(context: Context, private val fileName: String) : RoomDatabase.Callback() {
    private val context = context.applicationContext

    override fun onOpen(db: SupportSQLiteDatabase) {
        val markDao = RecordDatabase.getInstance(context).markDao()

        try {
            if (DataStore.Record.isMerged(context).not()) {
                markDao.merge(read(db) + readOthers())
                DataStore.Record.putMerged(context, true)
            }

            val marks = markDao.getAll()

            db.beginTransaction()

            try {
                db.execSQL("UPDATE verse SET bookmark = 0, favorite = 0, highlight_color = 0 WHERE bookmark != 0 OR favorite != 0 OR highlight_color != 0")

                marks.forEach {
                    db.execSQL(
                        "UPDATE verse SET bookmark = ?, favorite = ?, highlight_color = ? WHERE book = ? AND chapter = ? AND verse = ?",
                        arrayOf<Any>(if (it.bookmark) 1 else 0, if (it.favorite) 1 else 0, it.highlightColor, it.book, it.chapter, it.verse)
                    )
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            // 합치기에 실패하면 verse 표를 건드리지 않는다(남은 표시를 지우지 않게).
            Timber.e(e)
        }
    }

    private fun read(db: SupportSQLiteDatabase): List<Mark> = db.query(QUERY).use { it.marks() }

    // 다른 번역본 DB 파일. 열 수 없거나 옛 구조면 건너뛴다.
    private fun readOthers(): List<Mark> {
        val directory = context.getDatabasePath(PREFIX).parentFile ?: return emptyList()
        val files = directory.listFiles { file ->
            file.name.startsWith(PREFIX) &&
                    file.name.startsWith("$PREFIX$fileName:").not() &&
                    SUFFIXES.none { file.name.endsWith(it) }
        } ?: return emptyList()

        return files.flatMap { file ->
            try {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                    db.rawQuery(QUERY, null).use { it.marks() }
                }
            } catch (e: Exception) {
                Timber.w(e, "skip ${file.name}")
                emptyList()
            }
        }
    }

    private fun Cursor.marks(): List<Mark> {
        val marks = mutableListOf<Mark>()

        while (moveToNext()) {
            marks.add(Mark(getInt(0), getInt(1), getInt(2), getInt(3) != 0, getInt(4) != 0, getInt(5)))
        }

        return marks
    }

    companion object {
        private const val PREFIX = "${Application.PACKAGE_NAME}.persistence.database.Database."
        private val SUFFIXES = arrayOf("-shm", "-wal", "-journal")
        private const val QUERY = "SELECT book, chapter, verse, bookmark, favorite, highlight_color FROM verse WHERE bookmark != 0 OR favorite != 0 OR highlight_color != 0"
    }
}

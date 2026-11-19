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

            // 북마크 기능을 뺐다. 모은 뒤에 한 번, 북마크를 즐겨찾기로 옮긴다.
            if (DataStore.Record.isBookmarkMoved(context).not()) {
                markDao.moveBookmarksToFavorites()
                DataStore.Record.putBookmarkMoved(context, true)
            }

            val marks = markDao.getAll()

            db.beginTransaction()

            try {
                // verse 표엔 (책, 장, 절) 인덱스가 없어 표시마다 UPDATE 하면 표시 수만큼 표 전체를 훑는다.
                // 임시 표에 모아 두고 한 번에 칠한다.
                db.execSQL("CREATE TEMP TABLE IF NOT EXISTS mark_sync (book INTEGER NOT NULL, chapter INTEGER NOT NULL, verse INTEGER NOT NULL, bookmark INTEGER NOT NULL, favorite INTEGER NOT NULL, highlight_color INTEGER NOT NULL, PRIMARY KEY (book, chapter, verse))")
                db.execSQL("DELETE FROM mark_sync")

                db.compileStatement("INSERT INTO mark_sync VALUES (?, ?, ?, ?, ?, ?)").use { statement ->
                    marks.forEach {
                        statement.bindLong(1, it.book.toLong())
                        statement.bindLong(2, it.chapter.toLong())
                        statement.bindLong(3, it.verse.toLong())
                        statement.bindLong(4, if (it.bookmark) 1 else 0)
                        statement.bindLong(5, if (it.favorite) 1 else 0)
                        statement.bindLong(6, it.highlightColor.toLong())
                        statement.executeInsert()
                    }
                }

                db.execSQL("UPDATE verse SET bookmark = 0, favorite = 0, highlight_color = 0 WHERE bookmark != 0 OR favorite != 0 OR highlight_color != 0")
                db.execSQL("""
                    UPDATE verse SET
                        bookmark = (SELECT m.bookmark FROM mark_sync m WHERE m.book = verse.book AND m.chapter = verse.chapter AND m.verse = verse.verse),
                        favorite = (SELECT m.favorite FROM mark_sync m WHERE m.book = verse.book AND m.chapter = verse.chapter AND m.verse = verse.verse),
                        highlight_color = (SELECT m.highlight_color FROM mark_sync m WHERE m.book = verse.book AND m.chapter = verse.chapter AND m.verse = verse.verse)
                    WHERE EXISTS (SELECT 1 FROM mark_sync m WHERE m.book = verse.book AND m.chapter = verse.chapter AND m.verse = verse.verse)
                    """.trimIndent())
                db.execSQL("DROP TABLE mark_sync")

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

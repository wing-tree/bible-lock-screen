package com.duke.orca.android.kotlin.biblelockscreen.persistence.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Translation
import com.google.gson.Gson

// 루터 성경 DB 에 책 이름이 영어로 들어 있어, 열 때 루터 1912 판 이름으로 바꿔 둔다.
// 이미 기기에 복사된 DB 도 고쳐지도록 열 때마다 확인하고, 다를 때만 쓴다.
internal class BookNameFix(private val fileName: String) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        val names = BOOK_NAMES[fileName] ?: return
        val json = Gson().toJson(names)

        db.execSQL("UPDATE bible_book SET names = ? WHERE names != ?", arrayOf(json, json))
    }

    companion object {
        private val BOOK_NAMES = mapOf(
            Translation.FileName.LUTHER_BIBLE to arrayOf(
                "1. Mose", "2. Mose", "3. Mose", "4. Mose", "5. Mose", "Josua", "Richter", "Ruth",
                "1. Samuel", "2. Samuel", "1. Könige", "2. Könige", "1. Chronik", "2. Chronik",
                "Esra", "Nehemia", "Esther", "Hiob", "Psalmen", "Sprüche", "Prediger", "Hohelied",
                "Jesaja", "Jeremia", "Klagelieder", "Hesekiel", "Daniel", "Hosea", "Joel", "Amos",
                "Obadja", "Jona", "Micha", "Nahum", "Habakuk", "Zephanja", "Haggai", "Sacharja",
                "Maleachi", "Matthäus", "Markus", "Lukas", "Johannes", "Apostelgeschichte", "Römer",
                "1. Korinther", "2. Korinther", "Galater", "Epheser", "Philipper", "Kolosser",
                "1. Thessalonicher", "2. Thessalonicher", "1. Timotheus", "2. Timotheus", "Titus",
                "Philemon", "Hebräer", "Jakobus", "1. Petrus", "2. Petrus", "1. Johannes",
                "2. Johannes", "3. Johannes", "Judas", "Offenbarung"
            )
        )
    }
}

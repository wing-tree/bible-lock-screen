package com.duke.orca.android.kotlin.biblelockscreen.persistence.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Mark
import com.duke.orca.android.kotlin.biblelockscreen.persistence.dao.MarkDao

// 번역본 DB 와 따로 두는 기록 저장소. 번역본을 바꿔도 여기 남긴 표시를 새 DB 에 다시 칠한다(MarkSync).
@androidx.room.Database(
    entities = [Mark::class],
    exportSchema = true,
    version = 1
)
abstract class RecordDatabase: RoomDatabase() {
    abstract fun markDao(): MarkDao

    companion object {
        private const val NAME = "${Application.PACKAGE_NAME}.persistence.database.RecordDatabase"

        @Volatile
        private var INSTANCE: RecordDatabase? = null

        fun getInstance(context: Context): RecordDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecordDatabase::class.java,
                    NAME
                )
                    .allowMainThreadQueries()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

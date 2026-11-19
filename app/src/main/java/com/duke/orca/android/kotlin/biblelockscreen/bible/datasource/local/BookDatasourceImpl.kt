package com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local

import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Bible
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import javax.inject.Inject
import javax.inject.Provider

class BookDatasourceImpl @Inject constructor(private val databaseProvider: Provider<Database>) : BookDatasource {
    // 번역본을 바꾸면 DB 가 새로 갈아 끼워지고 옛 DB 는 닫히므로, 쓸 때마다 지금 DB 를 받는다.
    private val database: Database get() = databaseProvider.get()

    override fun get(): Bible {
        return database.bibleBookDao().get()
    }
}
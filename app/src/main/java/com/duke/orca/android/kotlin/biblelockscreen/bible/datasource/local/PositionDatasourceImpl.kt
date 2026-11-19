package com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local

import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Position
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import javax.inject.Inject
import javax.inject.Provider

class PositionDatasourceImpl @Inject constructor(private val databaseProvider: Provider<Database>) : PositionDatasource {
    // 번역본을 바꾸면 DB 가 새로 갈아 끼워지고 옛 DB 는 닫히므로, 쓸 때마다 지금 DB 를 받는다.
    private val database: Database get() = databaseProvider.get()

    override suspend fun insert(position: Position) {
        database.positionDao().insert(position)
    }

    override suspend fun get(book: Int, chapter: Int): Int? {
        return database.positionDao().get(book, chapter)?.value
    }
}
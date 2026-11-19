package com.duke.orca.android.kotlin.biblelockscreen.bible.repositories

import android.content.Context
import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.PositionDatasourceImpl
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Position
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database

interface PositionRepository {
    suspend fun insert(position: Position)
    suspend fun get(book: Int, chapter: Int): Int?

    companion object {
        fun from(context: Context): PositionRepository {
            val applicationContext = context.applicationContext

            return with(PositionDatasourceImpl { Database.getInstance(applicationContext) }) {
                PositionRepositoryImpl(this)
            }
        }
    }
}
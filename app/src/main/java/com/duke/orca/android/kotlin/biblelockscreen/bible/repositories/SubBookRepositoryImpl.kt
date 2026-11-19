package com.duke.orca.android.kotlin.biblelockscreen.bible.repositories

import android.app.Application
import android.content.Context
import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.BookDatasource
import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.SubBookDatasourceImpl
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Bible
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.SubDatabase
import javax.inject.Inject

class SubBookRepositoryImpl @Inject constructor(private val datasource: BookDatasource) : BookRepository {
    override fun get(): Bible {
        return datasource.get()
    }

    companion object {
        fun from(application: Application): BookRepository? {
            val database = SubDatabase.getInstance(application) ?: return null

            return SubBookRepositoryImpl(SubBookDatasourceImpl { SubDatabase.getInstance(application) ?: database })
        }
    }
}
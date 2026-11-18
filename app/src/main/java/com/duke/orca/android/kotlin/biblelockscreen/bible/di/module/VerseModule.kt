package com.duke.orca.android.kotlin.biblelockscreen.bible.di.module

import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.VerseDatasource
import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.VerseDatasourceImpl
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.VerseRepository
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.VerseRepositoryImpl
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.RecordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object VerseModule {
    @Provides
    fun provideVerseDatasource(database: Database, recordDatabase: RecordDatabase): VerseDatasource {
        return VerseDatasourceImpl(database, recordDatabase)
    }

    @Provides
    fun provideVerseRepository(datasource: VerseDatasource): VerseRepository {
        return VerseRepositoryImpl(datasource)
    }
}
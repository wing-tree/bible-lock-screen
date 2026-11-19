package com.duke.orca.android.kotlin.biblelockscreen.bible.di.module

import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.PositionDatasource
import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.PositionDatasourceImpl
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.PositionRepository
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.PositionRepositoryImpl
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Provider

@Module
@InstallIn(ViewModelComponent::class)
object PositionModule {
    @Provides
    fun providePositionDatasource(database: Provider<Database>): PositionDatasource {
        return PositionDatasourceImpl(database)
    }

    @Provides
    fun providePositionRepository(datasource: PositionDatasource): PositionRepository {
        return PositionRepositoryImpl(datasource)
    }
}
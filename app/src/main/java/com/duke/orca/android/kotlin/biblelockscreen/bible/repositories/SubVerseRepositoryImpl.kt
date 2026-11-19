package com.duke.orca.android.kotlin.biblelockscreen.bible.repositories

import android.app.Application
import androidx.annotation.ColorInt
import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.SubVerseDatasourceImpl
import com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local.VerseDatasource
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.SubDatabase
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SubVerseRepositoryImpl @Inject constructor(private val datasource: VerseDatasource) : VerseRepository {
    override fun get(id: Int): Flow<Verse> {
        return datasource.get(id)
    }

    override fun get(book: Int, chapter: Int): Flow<List<Verse>> {
        return datasource.get(book, chapter)
    }

    override fun get(book: Int, chapter: Int, verse: Int): Flow<Verse?> {
        return datasource.get(book, chapter, verse)
    }

    override fun getFavorites(): Flow<List<Verse>> {
        return datasource.getFavorites()
    }

    override fun search(text: String): Flow<List<Verse>> {
        return datasource.search(text)
    }

    override fun single(id: Int): Single<Verse> {
        return datasource.single(id)
    }

    override fun loadBookmarks(): Flowable<List<Verse>> {
        return datasource.loadBookmarks()
    }

    override fun loadHighlights(): Flowable<List<Verse>> {
        return datasource.loadHighlights()
    }

    override suspend fun getVerseCount(book: Int, chapter: Int): Int {
        return datasource.getVerseCount(book, chapter)
    }

    override suspend fun updateBookmark(id: Int, bookmark: Boolean) {
        datasource.updateBookmark(id, bookmark)
    }

    override suspend fun updateFavorite(id: Int, favorite: Boolean) {
        datasource.updateFavorite(id, favorite)
    }

    override suspend fun updateHighlightColor(id: Int, @ColorInt highlightColor: Int) {
        datasource.updateHighlightColor(id, highlightColor)
    }

    companion object {
        fun from(application: Application): VerseRepository? {
            val database = SubDatabase.getInstance(application) ?: return null

            return SubVerseRepositoryImpl(SubVerseDatasourceImpl { SubDatabase.getInstance(application) ?: database })
        }
    }
}
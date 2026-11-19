package com.duke.orca.android.kotlin.biblelockscreen.bible.datasource.local

import androidx.annotation.ColorInt
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.SubDatabase
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Provider

class SubVerseDatasourceImpl @Inject constructor(private val databaseProvider: Provider<SubDatabase>) : VerseDatasource {
    // 번역본을 바꾸면 DB 가 새로 갈아 끼워지고 옛 DB 는 닫히므로, 쓸 때마다 지금 DB 를 받는다.
    private val database: SubDatabase get() = databaseProvider.get()

    override fun get(id: Int): Flow<Verse> {
        return database.verseDao().get(id)
    }

    override fun get(book: Int, chapter: Int): Flow<List<Verse>> {
        return database.verseDao().get(book, chapter)
    }

    override fun get(book: Int, chapter: Int, verse: Int): Flow<Verse?> {
        return database.verseDao().get(book, chapter, verse)
    }

    override fun getFavorites(): Flow<List<Verse>> {
        return database.verseDao().getFavorites()
    }

    override fun search(text: String): Flow<List<Verse>> {
        return database.verseDao().search(text)
    }

    override fun single(id: Int): Single<Verse> {
        return database.verseDao().single(id)
    }

    override fun loadBookmarks(): Flowable<List<Verse>> {
        return database.verseDao().loadBookmarks()
    }

    override fun loadHighlights(): Flowable<List<Verse>> {
        return database.verseDao().loadHighlights()
    }

    override suspend fun getVerseCount(book: Int, chapter: Int): Int {
        return database.verseDao().getVerseCount(book, chapter)
    }

    override suspend fun updateBookmark(id: Int, bookmark: Boolean) {
        database.verseDao().updateBookmark(id, bookmark)
    }

    override suspend fun updateFavorite(id: Int, favorite: Boolean) {
        database.verseDao().updateFavorite(id, favorite)
    }

    override suspend fun updateHighlightColor(id: Int, @ColorInt highlightColor: Int) {
        database.verseDao().updateHighlightColor(id, highlightColor)
    }
}
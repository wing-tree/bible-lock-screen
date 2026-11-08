package com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.duke.orca.android.kotlin.biblelockscreen.base.viewmodels.BaseViewModel
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Font
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.BookRepository
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.SubBookRepositoryImpl
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.SubVerseRepositoryImpl
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.VerseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerseViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val verseRepository: VerseRepository,
    application: Application
) : BaseViewModel(application) {
    private val verse = MutableLiveData<Verse>()
    private val subVerse = MutableLiveData<Verse>()

    // 지금 지켜보는 대조 번역 구절. 즐겨찾기 등으로 구절이 다시 올 때마다 감시를 겹쳐 걸지 않는다.
    private var subVersePosition: Triple<Int, Int, Int>? = null

    val bible by lazy { bookRepository.get() }
    val subBible by lazy { subBookRepository?.get() }
    val triple = MediatorLiveData<Triple<Verse, Verse?, Font>?>().apply {
        addSource(font) {
            value = combine(font, verse, subVerse)
        }

        addSource(verse) {
            value = combine(font, verse, subVerse)
        }

        addSource(subVerse) {
            value = combine(font, verse, subVerse)
        }
    }

    private val subBookRepository by lazy {
        SubBookRepositoryImpl.from(application)
    }

    private val subVerseRepository by lazy {
        SubVerseRepositoryImpl.from(application)
    }

    private fun combine(
        source1: LiveData<Font>,
        source2: LiveData<Verse>,
        source3: LiveData<Verse>
    ): Triple<Verse, Verse?, Font>? {
        val font = source1.value ?: return null
        val verse = source2.value ?: return null
        val subVerse = source3.value

        return Triple(verse, subVerse, font)
    }

    fun loadVerseById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.get(id).flowOn(Dispatchers.IO).collect {
                it?.let {
                    // 화면에서 책 이름을 읽을 때 메인 스레드에서 쿼리하지 않도록 여기서 먼저 읽어 둔다.
                    bible
                    subBible
                    verse.postValue(it)
                    loadSubVerse(it.book, it.chapter, it.verse)
                }

            }
        }
    }

    fun loadSubVerse(book: Int, chapter: Int, verse: Int) {
        val position = Triple(book, chapter, verse)

        if (subVersePosition == position) return

        subVersePosition = position

        viewModelScope.launch(Dispatchers.IO) {
            subVerseRepository?.get(book, chapter, verse)?.flowOn(Dispatchers.IO)?.collect {
                subVerse.postValue(it)
            }
        }
    }

    fun updateFavorites(id: Int, favorites: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.updateFavorite(id, favorites)
        }
    }
}
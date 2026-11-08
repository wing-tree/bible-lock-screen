package com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels

import androidx.lifecycle.*
import com.duke.orca.android.kotlin.biblelockscreen.bible.adapters.VerseAdapter
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.BookRepository
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.VerseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val verseRepository: VerseRepository
) : ViewModel() {
    private val favorites = MutableLiveData<List<Verse>>()

    val adapterItems = favorites.map { verses ->
        verses.map {
            VerseAdapter.AdapterItem.Verse(
                id = it.id,
                book = it.book,
                chapter = it.chapter,
                verse = it.verse,
                word = it.word,
                bookmark = it.bookmark,
                favorite = it.favorite
            )
        }
    }

    val bibleBook by lazy { bookRepository.get() }

    fun loadFavorites() {
        viewModelScope.launch {
            verseRepository.getFavorites().collect {
                favorites.value = it
            }
        }
    }

    fun updateBookmark(id: Int, bookmark: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.updateBookmark(id, bookmark)
        }
    }

    fun updateFavorites(id: Int, favorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.updateFavorite(id, favorite)
        }
    }
}

package com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels

import androidx.lifecycle.*
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.BookRepository
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.VerseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BibleVerseSearchViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val verseRepository: VerseRepository,
) : ViewModel() {
    private val _searchResult = MutableLiveData<SearchResult>()
    val searchResult: LiveData<SearchResult> = _searchResult
    val bibleBook by lazy { bookRepository.get() }

    fun get(id: Int) = verseRepository.get(id).asLiveData(viewModelScope.coroutineContext)

    fun search(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val verses = verseRepository.search(text).first()

            withContext(Dispatchers.Main) {
                _searchResult.value = SearchResult(verses, text)
            }
        }
    }


    fun updateFavorite(id: Int, favorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.updateFavorite(id, favorite)
        }
    }
}

data class SearchResult(
    val searchResults: List<Verse>,
    val searchWord: String
)
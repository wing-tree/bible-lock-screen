package com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels

import androidx.lifecycle.*
import com.duke.orca.android.kotlin.biblelockscreen.bible.adapters.VerseAdapter
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.BookRepository
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.VerseRepository
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val verseRepository: VerseRepository
) : ViewModel() {
    private val favorites = MutableLiveData<List<Verse>>()
    private val nativeAds = MutableLiveData<List<NativeAd>>()

    val adapterItems = MediatorLiveData<List<VerseAdapter.AdapterItem>>().apply {
        addSource(favorites) {
            value = combine(favorites, nativeAds)
        }

        addSource(nativeAds) {
            value = combine(favorites, nativeAds)
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

    private fun combine(
        source1: LiveData<List<Verse>>,
        source2: LiveData<List<NativeAd>>
    ): List<VerseAdapter.AdapterItem> {
        val bibleVerses = source1.value ?: emptyList()
        val nativeAds = source2.value ?: emptyList()

        val adapterItems = arrayListOf<VerseAdapter.AdapterItem>()

        adapterItems.addAll(
            bibleVerses.map {
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
        )

        for (i in 0 until nativeAds.count()) {
            if (i == 0) {
                adapterItems.add(0, VerseAdapter.AdapterItem.NativeAdItem(-1, nativeAds[0]))
            } else {
                val index = i * AD_INTERVAL

                if (index <= adapterItems.count()) {
                    adapterItems.add(
                        index,
                        VerseAdapter.AdapterItem.NativeAdItem(-index, nativeAds[i])
                    )
                } else {
                    break
                }
            }
        }

        return adapterItems
    }

    companion object {
        private const val AD_INTERVAL = 8
    }
}
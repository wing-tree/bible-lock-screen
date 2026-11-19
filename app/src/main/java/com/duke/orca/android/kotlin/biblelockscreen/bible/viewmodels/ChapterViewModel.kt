package com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels

import android.app.Application
import androidx.annotation.ColorInt
import androidx.lifecycle.*
import com.duke.orca.android.kotlin.biblelockscreen.application.`is`
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.BLANK
import com.duke.orca.android.kotlin.biblelockscreen.bible.adapters.WordAdapter
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Font
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Position
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.repositories.*
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.datastore.PreferencesKeys
import com.duke.orca.android.kotlin.biblelockscreen.datastore.preferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ChapterViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val positionRepository: PositionRepository,
    private val verseRepository: VerseRepository,
    application: Application
) : AndroidViewModel(application) {
    private val preferencesDataStore = application.preferencesDataStore

    private val subBookRepository by lazy {
        SubBookRepositoryImpl.from(application)
    }

    private val subVerseRepository by lazy {
        SubVerseRepositoryImpl.from(application)
    }

    private val verses = MutableLiveData<List<Verse>>()
    private val subVerses = MutableLiveData<List<Verse>>()

    private val font = preferencesDataStore.data.distinctUntilChanged().mapLatest {
        val size = it[PreferencesKeys.Font.Bible.size] ?: DataStore.Font.DEFAULT_SIZE
        val textAlignment =
            it[PreferencesKeys.Font.Bible.textAlignment] ?: DataStore.Font.TextAlignment.LEFT

        Font(
            bold = false,
            size = size,
            textAlignment = textAlignment
        )
    }.asLiveData(Dispatchers.IO)

    val pair = MediatorLiveData<Pair<List<WordAdapter.AdapterItem>, Font>>().apply {
        addSource(font) {
            value = combine(verses, subVerses, font)
        }

        addSource(verses) {
            value = combine(verses, subVerses, font)
        }

        addSource(subVerses) {
            value = combine(verses, subVerses, font)
        }
    }

    val book by lazy { bookRepository.get() }
    private val subBook by lazy { subBookRepository?.get() }

    fun getVerses(book: Int, chapter: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.get(book, chapter).collect {
                verses.postValue(it)
            }
        }
    }

    fun getSubVerses(book: Int, chapter: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            subVerseRepository?.get(book, chapter)?.collect {
                subVerses.postValue(it)
            }
        }
    }


    fun updateFavorite(id: Int, favorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.updateFavorite(id, favorite)
        }
    }

    fun updateHighlightColor(id: Int, @ColorInt highlightColor: Int) {
        if (id.`is`(-1)) return

        viewModelScope.launch(Dispatchers.IO) {
            verseRepository.updateHighlightColor(id, highlightColor)
        }
    }

    fun getPosition(book: Int, chapter: Int): Int = runBlocking {
        positionRepository.get(book, chapter) ?: 0
    }

    fun insertPosition(book: Int, chapter: Int, value: Int) {
        viewModelScope.launch {
            positionRepository.insert(Position(book, chapter, value))
        }
    }

    private fun combine(
        source1: LiveData<List<Verse>>,
        source2: LiveData<List<Verse>>,
        source3: LiveData<Font>
    ): Pair<List<WordAdapter.AdapterItem>, Font> {
        val verses = source1.value ?: emptyList()
        val subVerses = source2.value ?: emptyList()
        val font = source3.value ?: Font.getDefaultFont()

        val adapterItems = arrayListOf<WordAdapter.AdapterItem>()
        val words = if (subVerses.isNotEmpty()) {
            verses.zip(subVerses) { verse, subVerse ->
                WordAdapter.AdapterItem.Word(
                    id = verse.id,
                    book = WordAdapter.AdapterItem.Word.Book(
                        verse.book,
                        book.name(verse.book)
                    ),
                    subBook = WordAdapter.AdapterItem.Word.Book(
                        subVerse.book,
                        subBook?.name(subVerse.book) ?: BLANK
                    ),
                    chapter = verse.chapter,
                    verse = verse.verse,
                    word = verse.word,
                    subWord = subVerse.word,
                    bookmark = verse.bookmark,
                    favorite = verse.favorite,
                    highlightColor = verse.highlightColor
                )
            }
        } else {
            verses.map { verse ->
                WordAdapter.AdapterItem.Word(
                    id = verse.id,
                    book = WordAdapter.AdapterItem.Word.Book(
                        verse.book,
                        book.name(verse.book)
                    ),
                    chapter = verse.chapter,
                    verse = verse.verse,
                    word = verse.word,
                    bookmark = verse.bookmark,
                    favorite = verse.favorite,
                    highlightColor = verse.highlightColor
                )
            }
        }

        adapterItems.addAll(words)

        return adapterItems to font
    }
}
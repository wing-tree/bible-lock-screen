package com.duke.orca.android.kotlin.biblelockscreen.settings.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duke.orca.android.kotlin.biblelockscreen.application.`is`
import com.duke.orca.android.kotlin.biblelockscreen.application.not
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Translation
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import java.util.*

class TranslationSelectionDialogViewModel(application: Application) : AndroidViewModel(application) {
    private val _items = MutableLiveData<List<Translation.Model>>()
    val items: LiveData<List<Translation.Model>>
        get() = _items

    private val _selectedItems = MutableLiveData<List<Translation.Model>>()
    val selectedItems: LiveData<List<Translation.Model>>
        get() = _selectedItems

    val selectedItemCount: Int
        get() = selectedItems.value?.count() ?: 0

    val currentFileName = DataStore.Translation.getFileName(application)
    val currentSubFileName = DataStore.Translation.getSubFileName(application)

    init {
        val items = listOf(
            with(Translation.FileName.AMERICAN_KING_JAMES_VERSION) {
                Translation.createFromFileName(
                    this,
                    `is`(currentFileName) or `is`(currentSubFileName)
                )
            },
            with(Translation.FileName.AMERICAN_STANDARD_VERSION) {
                Translation.createFromFileName(
                    this,
                    `is`(currentFileName) or `is`(currentSubFileName)
                )
            },
            with(Translation.FileName.KING_JAMES_VERSION) {
                Translation.createFromFileName(
                    this,
                    `is`(currentFileName) or `is`(currentSubFileName)
                )
            },
            with(Translation.FileName.UPDATED_KING_JAMES_VERSION) {
                Translation.createFromFileName(
                    this,
                    `is`(currentFileName) or `is`(currentSubFileName)
                )
            },
            with(Translation.FileName.KOREAN_REVISED_VERSION) {
                Translation.createFromFileName(
                    this,
                    `is`(currentFileName) or `is`(currentSubFileName)
                )
            },
            with(Translation.FileName.LOUIS_SEGOND) {
                Translation.createFromFileName(
                    this,
                    `is`(currentFileName) or `is`(currentSubFileName)
                )
            },
            with(Translation.FileName.LUTHER_BIBLE) {
                Translation.createFromFileName(
                    this,
                    `is`(currentFileName) or `is`(currentSubFileName)
                )
            }
        )

        val selectedItems = mutableListOf<Translation.Model>()

        items.find { it.fileName.`is`(currentFileName) }?.let {
            selectedItems.add(it)
        }

        items.find { it.fileName.`is`(currentSubFileName) }?.let {
            selectedItems.add(it)
        }

        _items.value = items
        _selectedItems.value = selectedItems
    }

    fun select(item: Translation.Model) {
        val items = items.value?.toMutableList() ?: return
        val selectedItems = selectedItems.value?.toMutableList() ?: mutableListOf()

        if (selectedItemCount > 1) {
            with(selectedItems.removeAt(0)) {
                val index = items.indexOf(this)

                if (index.not(-1)) {
                    items[index] = Translation.createFromFileName(fileName, false)
                }
            }
        }

        val index = items.indexOf(item)

        if (index.not(-1)) {
            val translation = Translation.createFromFileName(item.fileName, true)

            items[index] = translation
            selectedItems.add(translation)
        }

        _items.value = items
        _selectedItems.value = selectedItems
    }

    fun unselect(item: Translation.Model) {
        val items = items.value?.toMutableList() ?: return
        val selectedItems = selectedItems.value?.toMutableList() ?: mutableListOf()

        if (selectedItems.remove(item)) {
            val index = items.indexOf(item)

            if (index.not(-1)) {
                items[index] = Translation.createFromFileName(item.fileName, false)
            }
        }

        _items.value = items
        _selectedItems.value = selectedItems
    }

    fun swap(from: Int, to: Int) {
        val selectedItems = _selectedItems.value?.toMutableList() ?: return

        Collections.swap(selectedItems, from, to)

        _selectedItems.value = selectedItems
    }
}
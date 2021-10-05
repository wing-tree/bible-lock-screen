package com.duke.orca.android.kotlin.biblelockscreen.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.BLANK
import com.duke.orca.android.kotlin.biblelockscreen.application.SingleLiveEvent
import com.duke.orca.android.kotlin.biblelockscreen.application.SystemUiColorAction

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val books: Array<String> = application.resources.getStringArray(R.array.books)

    private val _systemUiColorChanged = SingleLiveEvent<SystemUiColorAction>()
    val systemUiColorChanged: LiveData<SystemUiColorAction> = _systemUiColorChanged

    private val _closeDrawer = SingleLiveEvent<Unit>()
    val closeDrawer: LiveData<Unit> = _closeDrawer

    fun getBook(book: Int): String {
        return if (book.dec() in 0 until books.count()) {
            books[book.dec()]
        } else {
            BLANK
        }
    }

    fun setSystemUiColor() {
        _systemUiColorChanged.value = SystemUiColorAction.SET
    }

    fun revertSystemUiColor() {
        _systemUiColorChanged.value = SystemUiColorAction.REVERT
    }

    fun callCloseDrawer() {
        _closeDrawer.call()
    }
}
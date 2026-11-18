package com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries

import androidx.annotation.ColorInt
import androidx.room.ColumnInfo
import androidx.room.Entity

// 한 구절에 남긴 책갈피·즐겨찾기·하이라이트. 번역본과 상관없이 장·절 위치로 남긴다.
@Entity(tableName = "mark", primaryKeys = ["book", "chapter", "verse"])
data class Mark(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val bookmark: Boolean = false,
    val favorite: Boolean = false,
    @ColumnInfo(name = "highlight_color")
    @ColorInt val highlightColor: Int = 0
) {
    val isEmpty: Boolean
        get() = bookmark.not() && favorite.not() && highlightColor == 0
}

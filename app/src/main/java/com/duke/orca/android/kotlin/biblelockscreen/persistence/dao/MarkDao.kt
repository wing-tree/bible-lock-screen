package com.duke.orca.android.kotlin.biblelockscreen.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Mark

@Dao
abstract class MarkDao {
    @Query("SELECT * FROM mark")
    abstract fun getAll(): List<Mark>

    @Query("SELECT * FROM mark WHERE book = :book AND chapter = :chapter AND verse = :verse LIMIT 1")
    abstract fun get(book: Int, chapter: Int, verse: Int): Mark?

    @Insert(onConflict = REPLACE)
    abstract fun insert(mark: Mark)

    @Delete
    abstract fun delete(mark: Mark)

    // 있던 표시에 바꿀 것만 얹는다. 다 비면 지운다.
    @Transaction
    open fun update(book: Int, chapter: Int, verse: Int, block: (Mark) -> Mark) {
        val mark = block(get(book, chapter, verse) ?: Mark(book, chapter, verse))

        if (mark.isEmpty) {
            delete(mark)
        } else {
            insert(mark)
        }
    }

    // 옛 번역본 DB 에서 모은 표시를 합친다. 책갈피·즐겨찾기는 하나라도 있으면 남기고, 색은 먼저 들어온 것을 둔다.
    @Transaction
    open fun merge(marks: List<Mark>) {
        marks.forEach { new ->
            update(new.book, new.chapter, new.verse) {
                it.copy(
                    bookmark = it.bookmark || new.bookmark,
                    favorite = it.favorite || new.favorite,
                    highlightColor = if (it.highlightColor != 0) it.highlightColor else new.highlightColor
                )
            }
        }
    }
}

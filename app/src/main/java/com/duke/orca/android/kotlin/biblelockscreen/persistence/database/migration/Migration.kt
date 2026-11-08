package com.duke.orca.android.kotlin.biblelockscreen.persistence.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE verse (
                    id INTEGER PRIMARY KEY NOT NULL,
                    book INTEGER NOT NULL,
                    bookmark INTEGER NOT NULL DEFAULT 0,
                    chapter INTEGER NOT NULL,
                    favorite INTEGER NOT NULL DEFAULT 0,
                    highlight_color INTEGER NOT NULL DEFAULT 0,
                    verse INTEGER NOT NULL,
                    word TEXT NOT NULL
                )
                """.trimIndent())
        db.execSQL("""
                INSERT INTO verse (id, book, bookmark, chapter, favorite, verse, word)
                SELECT id, book, bookmark, chapter, favorites, verse, word FROM bible_verse
                """.trimIndent())
        db.execSQL("DROP TABLE bible_verse")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE bible_chapter")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE position (
                    book INTEGER NOT NULL,
                    chapter INTEGER NOT NULL,
                    value INTEGER NOT NULL,
                    
                    CONSTRAINT primaryKeys PRIMARY KEY (book, chapter)
                )
                """.trimIndent())
    }
}
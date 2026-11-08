package com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels

import android.content.Context
import android.os.Build
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore

object Translation {
    data class Model(
        val abbreviation: String,
        val fileName: String,
        val isSelected: Boolean,
        val language: String,
        val name: String,
    )

    object Abbreviation {
        const val AMERICAN_KING_JAMES_VERSION = "AKJV"
        const val AMERICAN_STANDARD_VERSION = "ASV"
        const val KING_JAMES_VERSION = "KJV"
        const val KOREAN_REVISED_VERSION = "KRV"
        const val LOUIS_SEGOND = "LSG"
        const val LUTHER_BIBLE = "LUT"
        const val NEW_KOREAN_REVISED_VERSION = "NKRV"
        const val UPDATED_KING_JAMES_VERSION = "UKJV"
    }

    object FileName {
        const val AMERICAN_KING_JAMES_VERSION = "AmericanKingJamesVersion"
        const val AMERICAN_STANDARD_VERSION = "AmericanStandardVersion"
        const val KING_JAMES_VERSION = "KingJamesVersion"
        const val KOREAN_REVISED_VERSION = "KoreanRevisedVersion"
        const val LOUIS_SEGOND = "LouisSegond"
        const val LUTHER_BIBLE = "LutherBible"
        const val NEW_KOREAN_REVISED_VERSION = "NewKoreanRevisedVersion"
        const val UPDATED_KING_JAMES_VERSION = "UpdatedKingJamesVersion"
    }

    object Language {
        const val ENGLISH = "en"
        const val FILIPINO = "fil"
        const val FINNISH = "fi"
        const val FRENCH = "fr"
        const val GERMAN = "de"
        const val GREEK = "el"
        const val ITALIAN = "it"
        const val KOREAN = "ko"
        const val ROMANIAN = "ro"
        const val SPANISH = "es"

        private object DisplayName {
            const val ENGLISH = "English"
            const val FRENCH = "Français"
            const val GERMAN = "Deutsch"
            const val ITALIAN = "Italiano"
            const val KOREAN = "한국어"
        }

        fun String.toDisplayName(): String {
            return when(this) {
                ENGLISH -> DisplayName.ENGLISH
                FRENCH -> DisplayName.FRENCH
                GERMAN -> DisplayName.GERMAN
                ITALIAN -> DisplayName.ITALIAN
                KOREAN -> DisplayName.KOREAN
                else -> DisplayName.ENGLISH
            }
        }
    }

    object Name {
        const val AMERICAN_KING_JAMES_VERSION = "American King James Version"
        const val AMERICAN_STANDARD_VERSION = "American Standard Version"
        const val KING_JAMES_VERSION = "King James Version"
        const val KOREAN_REVISED_VERSION = "개역한글"
        const val LOUIS_SEGOND = "Louis Segond"
        const val LUTHER_BIBLE = "Lutherbibel"
        const val NEW_KOREAN_REVISED_VERSION = "개역개정"
        const val UPDATED_KING_JAMES_VERSION = "Updated King James Version"
    }

    fun getName(context: Context): String {
        return when (DataStore.Translation.getFileName(context)) {
            FileName.AMERICAN_KING_JAMES_VERSION -> Name.AMERICAN_KING_JAMES_VERSION
            FileName.AMERICAN_STANDARD_VERSION -> Name.AMERICAN_STANDARD_VERSION
            FileName.KING_JAMES_VERSION -> Name.KING_JAMES_VERSION
            FileName.KOREAN_REVISED_VERSION -> Name.KOREAN_REVISED_VERSION
            FileName.LOUIS_SEGOND -> Name.LOUIS_SEGOND
            FileName.LUTHER_BIBLE -> Name.LUTHER_BIBLE
            FileName.UPDATED_KING_JAMES_VERSION -> Name.UPDATED_KING_JAMES_VERSION
            FileName.NEW_KOREAN_REVISED_VERSION -> Name.NEW_KOREAN_REVISED_VERSION
            else -> getDisplayNameInLanguage(context)
        }
    }

    fun getFileNameInLanguage(context: Context): String {
        val language = context.resources.configuration.locales.get(0).language

        return when (language) {
            Language.ENGLISH -> FileName.KING_JAMES_VERSION
            Language.FRENCH -> FileName.LOUIS_SEGOND
            Language.GERMAN -> FileName.LUTHER_BIBLE
            Language.KOREAN -> FileName.KOREAN_REVISED_VERSION
            else -> FileName.KING_JAMES_VERSION
        }
    }

    private fun getAbbreviation(fileName: String): String {
        return when (fileName) {
            FileName.AMERICAN_KING_JAMES_VERSION -> Abbreviation.AMERICAN_KING_JAMES_VERSION
            FileName.AMERICAN_STANDARD_VERSION -> Abbreviation.AMERICAN_STANDARD_VERSION
            FileName.KING_JAMES_VERSION -> Abbreviation.KING_JAMES_VERSION
            FileName.KOREAN_REVISED_VERSION -> Abbreviation.KOREAN_REVISED_VERSION
            FileName.LOUIS_SEGOND -> Abbreviation.LOUIS_SEGOND
            FileName.LUTHER_BIBLE -> Abbreviation.LUTHER_BIBLE
            FileName.UPDATED_KING_JAMES_VERSION -> Abbreviation.UPDATED_KING_JAMES_VERSION
            FileName.NEW_KOREAN_REVISED_VERSION -> Abbreviation.NEW_KOREAN_REVISED_VERSION
            else -> throw IllegalArgumentException("IllegalArgumentException :$fileName")
        }
    }

    fun findNameByFileName(fileName: String): String {
        return when (fileName) {
            FileName.AMERICAN_KING_JAMES_VERSION -> Name.AMERICAN_KING_JAMES_VERSION
            FileName.AMERICAN_STANDARD_VERSION -> Name.AMERICAN_STANDARD_VERSION
            FileName.KING_JAMES_VERSION -> Name.KING_JAMES_VERSION
            FileName.KOREAN_REVISED_VERSION -> Name.KOREAN_REVISED_VERSION
            FileName.LOUIS_SEGOND -> Name.LOUIS_SEGOND
            FileName.LUTHER_BIBLE -> Name.LUTHER_BIBLE
            FileName.UPDATED_KING_JAMES_VERSION -> Name.UPDATED_KING_JAMES_VERSION
            FileName.NEW_KOREAN_REVISED_VERSION -> Name.NEW_KOREAN_REVISED_VERSION
            else -> throw IllegalArgumentException("IllegalArgumentException :$fileName")
        }
    }

    private fun getDisplayNameInLanguage(context: Context): String {
        val language = context.resources.configuration.locales.get(0).language

        return when (language) {
            Language.ENGLISH -> Name.KING_JAMES_VERSION
             Language.FRENCH -> Name.LOUIS_SEGOND
             Language.GERMAN -> Name.LUTHER_BIBLE
            Language.KOREAN -> Name.KOREAN_REVISED_VERSION
            else -> Name.KING_JAMES_VERSION
        }
    }

    private fun getLanguage(fileName: String): String {
        return when (fileName) {
            FileName.AMERICAN_KING_JAMES_VERSION -> Language.ENGLISH
            FileName.AMERICAN_STANDARD_VERSION -> Language.ENGLISH
            FileName.KING_JAMES_VERSION -> Language.ENGLISH
            FileName.KOREAN_REVISED_VERSION -> Language.KOREAN
            FileName.LOUIS_SEGOND -> Language.FRENCH
            FileName.LUTHER_BIBLE -> Language.GERMAN
            FileName.UPDATED_KING_JAMES_VERSION -> Language.ENGLISH
            FileName.NEW_KOREAN_REVISED_VERSION -> Language.KOREAN
            else -> throw IllegalArgumentException("IllegalArgumentException :$fileName")
        }
    }

    fun createFromFileName(fileName: String, isSelected: Boolean): Model {
        return Model(
            abbreviation = getAbbreviation(fileName),
            fileName = fileName,
            isSelected = isSelected,
            language = getLanguage(fileName),
            name = findNameByFileName(fileName)
        )
    }
}
package com.duke.orca.android.kotlin.biblelockscreen.persistence.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Bible
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Position
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Translation
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.persistence.dao.BibleBookDao
import com.duke.orca.android.kotlin.biblelockscreen.persistence.dao.PositionDao
import com.duke.orca.android.kotlin.biblelockscreen.persistence.dao.VerseDao
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.migration.MIGRATION_1_2
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.migration.MIGRATION_2_3
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.migration.MIGRATION_3_4
import com.duke.orca.android.kotlin.biblelockscreen.persistence.typeconverters.TypeConverters

@androidx.room.Database(
    entities = [Bible::class, Position::class, Verse::class],
    exportSchema = true,
    version = 4
)
@androidx.room.TypeConverters(TypeConverters::class)
abstract class Database: RoomDatabase() {
    abstract fun bibleBookDao(): BibleBookDao
    abstract fun positionDao(): PositionDao
    abstract fun verseDao(): VerseDao

    companion object {
        private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.persistence.database"
        private const val CLASS_NAME = "Database"
        private const val NAME = "$PACKAGE_NAME.$CLASS_NAME"
        private const val VERSION = "2.0.1"

        @Volatile
        private var INSTANCE: Database? = null

        fun getInstance(context: Context): Database {
            synchronized(this) {
                return INSTANCE ?: let {
                    getBuilder(context.applicationContext).build().also {
                        INSTANCE = it
                    }
                }
            }
        }

        fun refresh(context: Context) {
            synchronized(this) {
                clear()
                INSTANCE = getBuilder(context).build()
            }
        }

        private fun clear() {
            synchronized(this) {
                INSTANCE?.let {
                    if (it.isOpen) {
                        it.close()
                    }
                }

                INSTANCE = null
            }
        }

        private fun getBuilder(context: Context): Builder<Database> {
            val fileName: String = DataStore.Translation.getFileName(context).run {
                if (isBlank()) {
                    return@run Translation.getFileNameInLanguage(context).also {
                        DataStore.Translation.putFileName(context, it)
                    }
                }

                return@run this
            }

            return Room.databaseBuilder(
                context.applicationContext,
                Database::class.java,
                "$NAME.$fileName:$VERSION"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .allowMainThreadQueries()
                .createFromAsset("$fileName.db")
                .addCallback(BookNameFix(fileName))
        }
    }
}
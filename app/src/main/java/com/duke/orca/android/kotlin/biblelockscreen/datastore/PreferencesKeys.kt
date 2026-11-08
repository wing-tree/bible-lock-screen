package com.duke.orca.android.kotlin.biblelockscreen.datastore

import androidx.datastore.preferences.core.*
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application

object PreferencesKeys {
    private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.datastore"
    private const val OBJECT_NAME = "PreferencesKeys"

    val isFirstTime = booleanPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.isFirstTime")

    object Chapter {
        private const val OBJECT_NAME = "Chapter"
        val currentChapter = intPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.currentChapter")
    }

    object Verse {
        private const val OBJECT_NAME = "Verse"
        val currentVerse = intPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.currentVerse")
    }

    object Font {
        private const val OBJECT_NAME = "Font"
        val size = floatPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.size")
        val bold = booleanPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.bold")
        val textAlignment = intPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.textAlignment")

        object Bible {
            private const val OBJECT_NAME = "Bible"
            val size = floatPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.size")
            val textAlignment = intPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.textAlignment")
        }
    }

    object Display {
        private const val OBJECT_NAME = "Display"
        val isDarkMode = booleanPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.isDarkMode")
    }

    object LockScreen {
        private const val OBJECT_NAME = "LockScreen"
        val displayAfterUnlocking = booleanPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.displayAfterUnlocking")
        val showOnLockScreen = booleanPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.showOnLockScreen")
        val unlockWithBackKey = booleanPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.unlockWithBackKey")
    }

    object Translation {
        private const val OBJECT_NAME = "Translation"
        val fileName = stringPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.fileName")
        val subFileName = stringPreferencesKey("$PACKAGE_NAME.$OBJECT_NAME.subFileName")
    }
}
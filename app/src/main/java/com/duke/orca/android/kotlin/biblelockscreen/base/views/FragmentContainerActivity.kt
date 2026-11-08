package com.duke.orca.android.kotlin.biblelockscreen.base.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatDelegate
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.EXTRA_SIMPLE_NAME
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.EXTRA_RECREATE
import com.duke.orca.android.kotlin.biblelockscreen.application.notNull
import com.duke.orca.android.kotlin.biblelockscreen.base.viewmodels.FragmentContainerViewModel
import com.duke.orca.android.kotlin.biblelockscreen.bible.views.SearchFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.views.ChapterPagerFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.views.FavoritesFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.views.HighlightsFragment
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.FontSettingsFragment
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.LockScreenSettingsFragment
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable

@AndroidEntryPoint
class FragmentContainerActivity : BaseLockScreenActivity() {
    private val viewModel by viewModels<FragmentContainerViewModel>()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        setDarkMode(savedInstanceState)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_container)

        if (savedInstanceState.notNull) {
            val recreate = savedInstanceState?.getBoolean(Key.RECREATE)
            val intent = Intent().putExtra(EXTRA_RECREATE, recreate)

            setResult(RESULT_OK, intent)

            return
        }

        compositeDisposable.add(
            viewModel.activityResult.subscribe {
                setResult(it.resultCode, it.data)
            }
        )

        intent?.getStringExtra(EXTRA_SIMPLE_NAME)?.let {
            if (it.isBlank()) {
                finish()
            } else {
                replaceFragment(it)
            }
        } ?: finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(Key.RECREATE, true)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private fun setDarkMode(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            if (it.getBoolean(Key.RECREATE)) {
                if (DataStore.Display.isDarkMode(this)) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }

    @CallSuper
    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.z_adjustment_bottom, R.anim.slide_out_right)
    }

    private fun replaceFragment(simpleName: String) {
        val fragment = when (simpleName) {
            SearchFragment::class.java.simpleName -> SearchFragment()
            ChapterPagerFragment::class.java.simpleName -> ChapterPagerFragment()
            FavoritesFragment::class.java.simpleName -> FavoritesFragment()
            FontSettingsFragment::class.java.simpleName -> FontSettingsFragment()
            HighlightsFragment::class.java.simpleName -> HighlightsFragment()
            LockScreenSettingsFragment::class.java.simpleName -> LockScreenSettingsFragment()
            SettingsFragment::class.java.simpleName -> SettingsFragment()
            else -> {
                finish()
                return
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, fragment, fragment.tag)
            .commit()
    }

    companion object {
        private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.base.views"

        object Key {
            private const val OBJECT_NAME = "Key"
            const val RECREATE = "$PACKAGE_NAME.$OBJECT_NAME.RECREATE"
        }
    }
}
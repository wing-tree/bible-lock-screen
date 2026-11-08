package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseLockScreenActivity
import com.duke.orca.android.kotlin.biblelockscreen.databinding.ActivityPreferenceBinding
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.settings.adapters.PreferenceAdapter
import com.duke.orca.android.kotlin.biblelockscreen.settings.adapters.PreferenceAdapter.AdapterItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DisplaySettingsActivity : BaseLockScreenActivity() {
    private var _viewBinding: ActivityPreferenceBinding? = null
    private val viewBinding: ActivityPreferenceBinding
        get() = _viewBinding!!

    private val preferenceAdapter = PreferenceAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DataStore.Display.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        _viewBinding = ActivityPreferenceBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initData()
        bind()
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.z_adjustment_bottom, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        _viewBinding = null
        super.onDestroy()
    }

    private fun initData() {
        val isDarkMode = DataStore.Display.isDarkMode(this)

        val list: List<AdapterItem> = arrayListOf(
            AdapterItem.SwitchPreference(
                drawable = ContextCompat.getDrawable(this, R.drawable.ic_round_dark_mode_24),
                isChecked = isDarkMode,
                onCheckedChange = { isChecked ->
                    lifecycleScope.launch {
                        DataStore.Display.putDarkMode(this@DisplaySettingsActivity, isChecked)
                        delay(Duration.Delay.RECREATE)
                        recreate()
                    }
                },
                body = getString(R.string.dark_mode)
            )
        )

        preferenceAdapter.submitList(list)
    }

    private fun bind() {
        with(viewBinding.toolbar) {
            setTitle(R.string.display)

            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        viewBinding.recyclerView.apply {
            adapter = preferenceAdapter
            layoutManager = LinearLayoutManagerWrapper(context)
            setHasFixedSize(true)
        }
    }
}
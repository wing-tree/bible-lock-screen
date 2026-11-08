package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.views.PreferenceFragment
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.lockscreen.service.LockScreenService
import com.duke.orca.android.kotlin.biblelockscreen.settings.adapters.PreferenceAdapter.AdapterItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockScreenSettingsFragment : PreferenceFragment() {
    private object Id {
        const val DISPLAY_AFTER_UNLOCKING = 0L
    }

    override val toolbar by lazy { viewBinding.toolbar }
    override val toolbarTitleResId: Int = R.string.lock_screen

    private val items = arrayListOf<AdapterItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        initData()
        bind()

        return viewBinding.root
    }

    private fun initData() {
        val displayAfterUnlocking = DataStore.LockScreen.getDisplayAfterUnlocking(requireContext())
        val showOnLockScreen = DataStore.LockScreen.getShowOnLockScreen(requireContext())
        val unlockWithBackKey = DataStore.LockScreen.getUnlockWithBackKey(requireContext())

        with(items) {
            add(
                AdapterItem.SwitchPreference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_unlock_90px
                    ),
                    isChecked = showOnLockScreen,
                    onCheckedChange = { isChecked ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                DataStore.LockScreen.putShowOnLockScreen(requireContext(), isChecked)
                            }

                            if (isChecked) {
                                LockScreenService.start(requireContext())
                            } else {
                                LockScreenService.stop(requireContext())
                            }

                            with(items.toMutableList()) {
                                if (isChecked.not()) {
                                    removeAt(1)
                                }

                                preferenceAdapter.submitList(toList())
                            }
                        }
                    },
                    body = getString(R.string.show_on_lock_screen)
                )
            )

            add(
                AdapterItem.SwitchPreference(
                    drawable = null,
                    id = Id.DISPLAY_AFTER_UNLOCKING,
                    isChecked = displayAfterUnlocking,
                    isClickable = showOnLockScreen,
                    onCheckedChange = { isChecked ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            DataStore.LockScreen.putDisplayAfterUnlocking(requireContext(), isChecked)
                        }
                    },
                    body = getString(R.string.display_after_unlocking)
                )
            )

            add(
                AdapterItem.SwitchPreference(
                    drawable = null,
                    isChecked = unlockWithBackKey,
                    onCheckedChange = { isChecked ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            DataStore.LockScreen.putUnlockWithBackKey(requireContext(), isChecked)
                        }
                    },
                    body = getString(R.string.unlock_with_back_button)
                )
            )
        }

        with(items.toMutableList()) {
            if (showOnLockScreen.not()) {
                removeAt(1)
            }

            preferenceAdapter.submitList(toList())
        }
    }

    private fun bind() {
        viewBinding.recyclerView.apply {
            adapter = preferenceAdapter
            layoutManager = LinearLayoutManagerWrapper(requireContext())
            setHasFixedSize(true)
        }
    }
}
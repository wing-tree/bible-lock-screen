package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
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

class LockScreenSettingsFragment : PreferenceFragment(),
    VerseOrderChoiceDialogFragment.OnItemClickListener {
    private object Id {
        const val DISPLAY_AFTER_UNLOCKING = 0L
        const val VERSE_ORDER = 1L
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
        val verseOrder = DataStore.LockScreen.getVerseOrder(requireContext())

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

            add(
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_format_list_bulleted_24
                    ),
                    id = Id.VERSE_ORDER,
                    onClick = {
                        VerseOrderChoiceDialogFragment().also {
                            it.show(childFragmentManager, it.tag)
                        }
                    },
                    summary = getString(VerseOrderChoiceDialogFragment.nameOf(verseOrder)),
                    body = getString(R.string.verse_order)
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

    override fun onVerseOrderClick(dialogFragment: DialogFragment, item: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            DataStore.LockScreen.putVerseOrder(requireContext(), item)
        }

        preferenceAdapter.updateSummary(Id.VERSE_ORDER, getString(VerseOrderChoiceDialogFragment.nameOf(item)))
        dialogFragment.dismiss()
    }

    private fun bind() {
        viewBinding.recyclerView.apply {
            adapter = preferenceAdapter
            layoutManager = LinearLayoutManagerWrapper(requireContext())
            setHasFixedSize(true)
        }
    }
}
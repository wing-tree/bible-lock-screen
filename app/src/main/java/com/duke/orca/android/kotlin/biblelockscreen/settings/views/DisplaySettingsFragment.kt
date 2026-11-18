package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.views.PreferenceFragment
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.settings.adapters.PreferenceAdapter.AdapterItem

class DisplaySettingsFragment : PreferenceFragment(),
    NightModeChoiceDialogFragment.OnItemClickListener {
    private object Id {
        const val NIGHT_MODE = 0L
    }

    override val toolbar by lazy { viewBinding.toolbar }
    override val toolbarTitleResId: Int = R.string.display

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
        val nightMode = DataStore.Display.getNightMode(requireContext())

        with(items) {
            add(
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_dark_mode_24),
                    id = Id.NIGHT_MODE,
                    onClick = {
                        NightModeChoiceDialogFragment().also {
                            it.show(childFragmentManager, it.tag)
                        }
                    },
                    summary = getString(NightModeChoiceDialogFragment.nameOf(nightMode)),
                    body = getString(R.string.dark_mode)
                )
            )
        }

        preferenceAdapter.submitList(items.toList())
    }

    override fun onNightModeClick(dialogFragment: DialogFragment, item: Int) {
        preferenceAdapter.updateSummary(Id.NIGHT_MODE, getString(NightModeChoiceDialogFragment.nameOf(item)))
        dialogFragment.dismiss()

        delayOnLifecycle(Duration.Delay.RECREATE) {
            DataStore.Display.putNightMode(requireContext(), item)
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
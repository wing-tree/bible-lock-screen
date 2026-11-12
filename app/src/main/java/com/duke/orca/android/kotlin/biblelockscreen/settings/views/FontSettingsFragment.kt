package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.views.PreferenceFragment
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.settings.adapters.PreferenceAdapter.AdapterItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FontSettingsFragment : PreferenceFragment(),
    FontSizeChoiceDialogFragment.OnItemClickListener,
    TextAlignmentChoiceDialogFragment.OnItemClickListener {
    private object Id {
        const val FONT_SIZE = 0L
        const val TEXT_ALIGNMENT = 1L
    }

    override val toolbar by lazy { viewBinding.toolbar }
    override val toolbarTitleResId: Int = R.string.font

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

    override fun onItemClick(dialogFragment: DialogFragment, item: Float) {
        lifecycleScope.launch(Dispatchers.IO) {
            DataStore.Font.putSize(requireContext(), item)
        }

        preferenceAdapter.updateSummary(Id.FONT_SIZE, String.format("%.0f", item))

        delayOnLifecycle(Duration.Delay.DISMISS) {
            dialogFragment.dismiss()
        }
    }

    override fun onItemClick(dialogFragment: DialogFragment, item: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            DataStore.Font.putTextAlignment(requireContext(), item)
        }

        preferenceAdapter.updateDrawable(Id.TEXT_ALIGNMENT, getTextAlignmentDrawable(item))
        preferenceAdapter.updateSummary(Id.TEXT_ALIGNMENT, getTextAlignmentSummary(item))

        delayOnLifecycle(Duration.Delay.DISMISS) {
            dialogFragment.dismiss()
        }
    }

    private fun initData() {
        val fontSize = DataStore.Font.getSize(requireContext())
        val bold = DataStore.Font.getBold(requireContext())
        val textAlignment = DataStore.Font.getTextAlignment(requireContext())

        val list: List<AdapterItem> = arrayListOf(
            AdapterItem.Preference(
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_format_size_24),
                id = Id.FONT_SIZE,
                summary = String.format("%.0f", fontSize),
                onClick = {
                    FontSizeChoiceDialogFragment().also {
                        it.show(childFragmentManager, it.tag)
                    }
                },
                body = getString(R.string.font_size)
            ),
            AdapterItem.SwitchPreference(
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_format_bold_24),
                isChecked = bold,
                onCheckedChange = { isChecked ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        DataStore.Font.putBold(requireContext(), isChecked)
                    }
                },
                body = getString(R.string.bold)
            ),
            AdapterItem.Preference(
                drawable = getTextAlignmentDrawable(textAlignment),
                id = Id.TEXT_ALIGNMENT,
                onClick = {
                    TextAlignmentChoiceDialogFragment().also {
                        it.show(childFragmentManager, it.tag)
                    }
                },
                summary = getTextAlignmentSummary(textAlignment),
                body = getString(R.string.text_alignment)
            ),
        )

        preferenceAdapter.submitList(list)
    }

    private fun bind() {
        viewBinding.recyclerView.apply {
            adapter = preferenceAdapter
            layoutManager = LinearLayoutManagerWrapper(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun getTextAlignmentDrawable(textAlignment: Int): Drawable? {
        @DrawableRes
        val id = when(textAlignment) {
            DataStore.Font.TextAlignment.LEFT -> R.drawable.ic_round_format_align_left_24
            DataStore.Font.TextAlignment.CENTER -> R.drawable.ic_round_format_align_center_24
            DataStore.Font.TextAlignment.RIGHT -> R.drawable.ic_round_format_align_right_24
            else -> throw IllegalArgumentException()
        }

        return ContextCompat.getDrawable(requireContext(), id)
    }

    private fun getTextAlignmentSummary(textAlignment: Int): String {
        @StringRes
        val id = when(textAlignment) {
            DataStore.Font.TextAlignment.LEFT -> R.string.align_left
            DataStore.Font.TextAlignment.CENTER -> R.string.align_center
            DataStore.Font.TextAlignment.RIGHT -> R.string.align_right
            else -> throw IllegalArgumentException()
        }

        return getString(id)
    }
}
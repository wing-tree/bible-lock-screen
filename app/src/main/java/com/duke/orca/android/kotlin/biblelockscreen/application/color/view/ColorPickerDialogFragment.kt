package com.duke.orca.android.kotlin.biblelockscreen.application.color.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.color.adapter.ColorAdapter
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Key
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentColorPickerDialogBinding

class ColorPickerDialogFragment : BaseDialogFragment<FragmentColorPickerDialogBinding>() {
    override val setWindowAnimation: Boolean
        get() = false

    private val pickedColor by lazy {
        arguments?.getInt(Key.PICKED_COLOR) ?: 0
    }

    private val colorAdapter by lazy {
        ColorAdapter(
            resources.getIntArray(R.array.highlight_colors),
            pickedColor,
        ) {
            onColorPickedListener?.onColorPicked(this, it)
        }
    }

    private var onColorPickedListener: OnColorPickedListener? = null

    interface OnColorPickedListener {
        fun onColorPicked(dialogFragment: DialogFragment, @ColorInt color: Int)
    }

    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentColorPickerDialogBinding {
        return FragmentColorPickerDialogBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        with(parentFragment) {
            if (this is OnColorPickedListener) {
                onColorPickedListener = this
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        bind()

        return viewBinding.root
    }

    private fun bind() {
        with(viewBinding) {
            recyclerView.apply {
                adapter = colorAdapter
                layoutManager = GridLayoutManager(context, SPAN_COUNT).apply {
                    isItemPrefetchEnabled = true
                    initialPrefetchItemCount = colorAdapter.itemCount
                }
            }

            textViewRemove.isVisible = pickedColor != 0
            textViewRemove.setOnClickListener {
                onColorPickedListener?.onColorPicked(this@ColorPickerDialogFragment, 0)
            }
        }
    }

    companion object {
        private const val SPAN_COUNT = 6

        fun newInstance(@ColorInt pickedColor: Int): ColorPickerDialogFragment {
            return ColorPickerDialogFragment().apply {
                val bundle = Bundle()

                bundle.putInt(Key.PICKED_COLOR, pickedColor)

                arguments = bundle
            }
        }
    }
}
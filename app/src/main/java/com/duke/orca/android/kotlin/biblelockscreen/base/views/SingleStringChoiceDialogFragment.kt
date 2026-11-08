package com.duke.orca.android.kotlin.biblelockscreen.base.views

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.databinding.SingleChoiceItemBinding

class SingleStringChoiceDialogFragment : SingleChoiceDialogFragment<String>() {
    interface OnItemChoiceListener {
        fun onItemChoice(dialogFragment: DialogFragment, item: String)
    }

    private var onItemChoiceListener: OnItemChoiceListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragment?.let {
            if (it is OnItemChoiceListener) {
                this.onItemChoiceListener = it
            }
        }
    }

    override fun bind(viewBinding: SingleChoiceItemBinding, item: String) {
        viewBinding.root.text = item
        viewBinding.root.setOnClickListener {
            onItemChoiceListener?.onItemChoice(this, item)
        }
    }

    override val items: Array<String> by lazy { arguments?.getStringArray(Key.ITEMS) ?: emptyArray() }

    companion object {
        private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.base.views"

        private object Key {
            const val ITEMS = "$PACKAGE_NAME.ITEMS"
        }

        fun newInstance(items: Array<String>): SingleStringChoiceDialogFragment {
            return SingleStringChoiceDialogFragment().apply {
                arguments = Bundle().apply { putStringArray(Key.ITEMS, items) }
            }
        }
    }
}
package com.duke.orca.android.kotlin.biblelockscreen.bible.views

import androidx.core.os.BundleCompat
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.base.views.SingleChoiceDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.databinding.SingleChoiceItemBinding

class OptionChoiceDialogFragment : SingleChoiceDialogFragment<String>() {
    interface OnOptionChoiceListener {
        fun onOptionChoice(dialogFragment: DialogFragment, option: String, content: Content)
    }

    private val content by lazy { arguments?.let { BundleCompat.getParcelable(it, Key.CONTENT, Content::class.java) } }

    private var onOptionChoiceListener: OnOptionChoiceListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragment?.let {
            if (it is OnOptionChoiceListener) {
                this.onOptionChoiceListener = it
            }
        }
    }

    override fun bind(viewBinding: SingleChoiceItemBinding, item: String) {
        viewBinding.root.text = item
        viewBinding.root.setOnClickListener {
            delayOnLifecycle(Duration.Delay.DISMISS) {
                content?.let {
                    onOptionChoiceListener?.onOptionChoice(this, item, it)
                } ?: dismiss()
            }
        }
    }

    override val items: Array<String> by lazy { arguments?.getStringArray(Key.ITEMS) ?: emptyArray() }

    companion object {
        private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.bibleverses.views"

        private object Key {
            const val CONTENT = "$PACKAGE_NAME.CONTENT"
            const val ITEMS = "$PACKAGE_NAME.ITEMS"
        }

        fun newInstance(items: Array<String>, content: Content): OptionChoiceDialogFragment {
            return OptionChoiceDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Key.CONTENT, content)
                    putStringArray(Key.ITEMS, items)
                }
            }
        }
    }
}
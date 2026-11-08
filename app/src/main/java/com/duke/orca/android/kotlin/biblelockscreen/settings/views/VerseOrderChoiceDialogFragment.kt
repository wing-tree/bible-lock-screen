package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseListDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.databinding.TextAlignmentChoiceItemBinding
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore.LockScreen.VerseOrder

// 잠금화면 구절 순서를 고르는 창. 글자 정렬 창과 같은 줄 모양을 쓰되 아이콘은 숨긴다.
class VerseOrderChoiceDialogFragment : BaseListDialogFragment<TextAlignmentChoiceItemBinding, Int>() {
    private var onItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onVerseOrderClick(dialogFragment: DialogFragment, item: Int)
    }

    override fun inflateItemView(
        inflater: LayoutInflater,
        container: ViewGroup
    ): TextAlignmentChoiceItemBinding {
        return TextAlignmentChoiceItemBinding.inflate(inflater, container, false)
    }

    override fun bind(viewBinding: TextAlignmentChoiceItemBinding, item: Int) {
        viewBinding.imageView.isVisible = false
        viewBinding.textView.text = getString(nameOf(item))

        viewBinding.root.setOnClickListener {
            onItemClickListener?.onVerseOrderClick(this, item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragment?.let {
            if (it is OnItemClickListener) {
                this.onItemClickListener = it
            }
        }
    }

    override val items: Array<Int> = arrayOf(
        VerseOrder.SEQUENTIAL,
        VerseOrder.DAILY,
        VerseOrder.FAVORITES
    )

    companion object {
        fun nameOf(verseOrder: Int): Int = when (verseOrder) {
            VerseOrder.DAILY -> R.string.verse_order_daily
            VerseOrder.FAVORITES -> R.string.verse_order_favorites
            else -> R.string.verse_order_sequential
        }
    }
}

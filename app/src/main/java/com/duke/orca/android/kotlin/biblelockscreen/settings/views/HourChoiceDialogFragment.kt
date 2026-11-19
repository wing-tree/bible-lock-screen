package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseListDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.dailyverse.DailyVerseNotice
import com.duke.orca.android.kotlin.biblelockscreen.databinding.TextAlignmentChoiceItemBinding

// 오늘의 말씀 알림 시각(정각)을 고르는 창. 구절 순서 창과 같은 줄 모양을 쓴다.
class HourChoiceDialogFragment : BaseListDialogFragment<TextAlignmentChoiceItemBinding, Int>() {
    private var onItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onHourClick(dialogFragment: DialogFragment, item: Int)
    }

    override fun inflateItemView(
        inflater: LayoutInflater,
        container: ViewGroup
    ): TextAlignmentChoiceItemBinding {
        return TextAlignmentChoiceItemBinding.inflate(inflater, container, false)
    }

    override fun bind(viewBinding: TextAlignmentChoiceItemBinding, item: Int) {
        viewBinding.imageView.isVisible = false
        viewBinding.textView.text = DailyVerseNotice.format(item)

        viewBinding.root.setOnClickListener {
            onItemClickListener?.onHourClick(this, item)
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

    // 새벽은 빼고 아침 5시부터 밤 11시까지.
    override val items: Array<Int> = (5..23).toList().toTypedArray()
}

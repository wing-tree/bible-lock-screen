package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseListDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.databinding.TextAlignmentChoiceItemBinding

// 다크 모드(시스템 설정 따르기·라이트·다크)를 고르는 창. 구절 순서 창과 같은 모양.
class NightModeChoiceDialogFragment : BaseListDialogFragment<TextAlignmentChoiceItemBinding, Int>() {
    private var onItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onNightModeClick(dialogFragment: DialogFragment, item: Int)
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
            onItemClickListener?.onNightModeClick(this, item)
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
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES
    )

    companion object {
        fun nameOf(nightMode: Int): Int = when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.string.night_mode_light
            AppCompatDelegate.MODE_NIGHT_YES -> R.string.night_mode_dark
            else -> R.string.night_mode_system
        }
    }
}

package com.duke.orca.android.kotlin.biblelockscreen.bible.views

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.BLANK
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.application.fadeIn
import com.duke.orca.android.kotlin.biblelockscreen.application.hide
import com.duke.orca.android.kotlin.biblelockscreen.application.isNotVisible
import com.duke.orca.android.kotlin.biblelockscreen.application.show
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.copyToClipboard
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Font
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.share
import com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels.VerseViewModel
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentVerseBinding
import com.like.LikeButton
import com.like.OnLikeListener
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class VerseFragment : BaseFragment<FragmentVerseBinding>(),
    OptionChoiceDialogFragment.OnOptionChoiceListener {
    private val viewModel by viewModels<VerseViewModel>()
    private val options by lazy { arrayOf(getString(R.string.copy), getString(R.string.share)) }
    private val onResumed = AtomicBoolean(false)

    private var currentFont: Font? = null

    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentVerseBinding {
        return FragmentVerseBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        observe(viewBinding)

        val id = arguments?.getInt(Key.ID) ?: 0

        viewModel.loadVerseById(id)

        return viewBinding.root
    }

    override fun onResume() {
        super.onResume()
        onResumed.set(true)
    }

    override fun onPause() {
        onResumed.set(false)
        super.onPause()
    }

    private fun observe(binding: FragmentVerseBinding) {
        viewModel.triple.observe(viewLifecycleOwner) { triple ->
            triple?.let { (verse, subVerse, font) ->
                val typeface = binding.textViewWord.typeface

                with(font) {
                    currentFont?.let {
                        if (it.contentEquals(this)) {
                            return@with
                        }
                    }

                    binding.textViewWord.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
                    binding.textViewWord.typeface =
                        Typeface.create(typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
                    binding.textViewWord.gravity = textAlignment

                    binding.textViewSubWord.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
                    binding.textViewSubWord.typeface =
                        Typeface.create(typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
                    binding.textViewSubWord.gravity = textAlignment

                    with(
                        size.minus(
                            when {
                                size > 16.0f -> 4
                                size > 24.0f -> 8
                                else -> 2
                            }
                        )
                    ) {
                        binding.textViewBook.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)
                        binding.textViewChapter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)
                        binding.textViewColon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)
                        binding.textViewVerse.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)

                        binding.textViewSubBook.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)
                        binding.textViewSubChapter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)
                        binding.textViewSubColon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)
                        binding.textViewSubVerse.setTextSize(TypedValue.COMPLEX_UNIT_DIP, this)
                    }

                    currentFont = this
                }

                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = font.textAlignment
                    binding.linearLayout.layoutParams = this
                    binding.linearLayoutSub.layoutParams = this
                }

                bind(binding, verse, subVerse)
            }
        }
    }

    private fun bind(binding: FragmentVerseBinding, verse: Verse, subVerse: Verse?) {
        binding.textViewWord.text = verse.word
        binding.textViewBook.text = viewModel.bible.name(verse.book)
        binding.textViewChapter.text = "${verse.chapter}"
        binding.textViewVerse.text = "${verse.verse}"

        subVerse?.let {
            binding.textViewSubWord.show()
            binding.linearLayoutSub.show()

            binding.textViewSubWord.text = it.word
            binding.textViewSubBook.text = viewModel.subBible?.name(it.book) ?: BLANK
            binding.textViewSubChapter.text = "${it.chapter}"
            binding.textViewSubVerse.text = "${it.verse}"
        } ?: run {
            binding.textViewSubWord.hide()
            binding.linearLayoutSub.hide()
        }

        binding.likeButton.isLiked = verse.favorite
        binding.likeButton.setOnLikeListener(object : OnLikeListener {
            override fun liked(likeButton: LikeButton?) {
                addToFavorites(verse.id)
            }

            override fun unLiked(likeButton: LikeButton?) {
                deleteFavorites(verse.id)
            }
        })

        binding.imageViewMoreVert.setOnClickListener {
            OptionChoiceDialogFragment.newInstance(options, verse.content).also {
                it.show(childFragmentManager, it.tag)
            }
        }

        with(binding.nestedScrollView) {
            if (isNotVisible) {
                if (onResumed.get()) {
                    delayOnLifecycle(Duration.Delay.SCROLL) {
                        fadeIn(Duration.Animation.FADE_IN)
                    }
                } else {
                    show()
                }
            }
        }
    }

    private fun addToFavorites(id: Int) {
        viewModel.updateFavorites(id, true)
    }

    private fun deleteFavorites(id: Int) {
        viewModel.updateFavorites(id, false)
    }

    override fun onOptionChoice(
        dialogFragment: DialogFragment,
        option: String,
        content: Content
    ) {
        when(option) {
            options[0] -> {
                copyToClipboard(requireContext(), viewModel.bible, content)
                dialogFragment.dismiss()
            }
            options[1] -> {
                share(requireContext(), viewModel.bible,  content)
                dialogFragment.dismiss()
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.bible.views"

        private object Key {
            const val ID = "$PACKAGE_NAME.ID"
        }

        fun newInstance(id: Int): VerseFragment {
            return VerseFragment().apply {
                arguments = Bundle().apply {
                    putInt(Key.ID, id)
                }
            }
        }
    }
}
package com.duke.orca.android.kotlin.biblelockscreen.bible.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.color.view.ColorPickerDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Application
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.application.fadeIn
import com.duke.orca.android.kotlin.biblelockscreen.application.isNonZero
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.views.ViewStubFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.adapters.WordAdapter
import com.duke.orca.android.kotlin.biblelockscreen.bible.copyToClipboard
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.share
import com.duke.orca.android.kotlin.biblelockscreen.bible.shareAsImage
import com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels.ChapterViewModel
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentChapterBinding
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import timber.log.Timber

@FlowPreview
@AndroidEntryPoint
class ChapterFragment : ViewStubFragment(),
    ColorPickerDialogFragment.OnColorPickedListener,
    OptionChoiceDialogFragment.OnOptionChoiceListener
{
    override val layoutResource: Int
        get() = R.layout.fragment_chapter

    private val viewModel by viewModels<ChapterViewModel>()
    private val book by lazy { arguments?.getInt(Key.BOOK) ?: 1 }
    private val chapter by lazy { arguments?.getInt(Key.CHAPTER) ?: 1 }

    private val wordAdapter by lazy {
        WordAdapter(requireContext()).apply {
            setOnOptionsItemSelectedListener(object : WordAdapter.OnOptionsItemSelectedListener {
                override fun onOptionsItemSelected(
                    item: WordAdapter.AdapterItem.Word,
                    optionsItem: WordAdapter.OptionsItem
                ) {
                    when(optionsItem) {
                        is WordAdapter.OptionsItem.Highlight -> {
                            ColorPickerDialogFragment.newInstance(item.highlightColor).also {
                                it.show(childFragmentManager, it.tag)
                            }
                        }
                        is WordAdapter.OptionsItem.Bookmark -> viewModel.updateBookmark(item.id, optionsItem.liked)
                        is WordAdapter.OptionsItem.Favorite -> viewModel.updateFavorite(item.id, optionsItem.liked)
                        is WordAdapter.OptionsItem.More -> {
                            OptionChoiceDialogFragment.newInstance(options, item.content).also {
                                it.show(childFragmentManager, it.tag)
                            }
                        }
                    }
                }
            })
        }
    }

    private val options by lazy { arrayOf(getString(R.string.copy), getString(R.string.share), getString(R.string.share_as_image)) }

    private var _viewBinding: FragmentChapterBinding? = null
    private val viewBinding: FragmentChapterBinding get() = _viewBinding!!

    override fun onInflate(view: View) {
        _viewBinding = FragmentChapterBinding.bind(view)

//        viewModel.highlightColor.observe(viewLifecycleOwner, { highlightColor ->
//            wordAdapter.setHighlightColor(highlightColor) { adapterItem ->
//                adapterItem.also {
//                    if (it is WordAdapter.AdapterItem.Word && it.highlightColor.isNonZero()) {
//                        viewModel.updateHighlightColor(it.id, highlightColor)
//                    }
//                }
//            }
//        })

        with(viewBinding) {
            val position = viewModel.getPosition(book, chapter)

            recyclerViewChapter.apply {
                adapter = wordAdapter
                layoutManager = LinearLayoutManagerWrapper(requireContext())
                setHasFixedSize(true)

                with(itemAnimator) {
                    if (this is SimpleItemAnimator) {
                        supportsChangeAnimations = false
                    }
                }

                delayOnLifecycle(Duration.Delay.SCROLL) {
                    scrollToPosition(position)
                    fadeIn(Duration.Animation.FADE_IN)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        initData()
        observe()

        return viewStubBinding.root
    }

    override fun onPause() {
        insertPosition()
        super.onPause()
    }

    private fun insertPosition() {
        try {
            val layoutManager = viewBinding.recyclerViewChapter.layoutManager

            if (layoutManager is LinearLayoutManager) {
                val position = layoutManager.findLastCompletelyVisibleItemPosition()

                viewModel.insertPosition(book, chapter, position)
            }
        } catch (e: NullPointerException) {
            Timber.e(e)
        }
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }


    override fun onColorPicked(dialogFragment: DialogFragment, @ColorInt color: Int) {
        viewModel.updateHighlightColor(wordAdapter.currentFocusedItem.id, color)

        delayOnLifecycle(Duration.Delay.DISMISS) {
            dialogFragment.dismiss()
        }
    }

    private fun initData() {
        viewModel.getVerses(book, chapter)
        viewModel.getSubVerses(book, chapter)
    }

    private fun observe() {
        viewModel.pair.observe(viewLifecycleOwner, { (adapterItems, font) ->
            wordAdapter.submitList(adapterItems) {
                wordAdapter.setFont(font)
            }
        })
    }

    override fun onOptionChoice(
        dialogFragment: DialogFragment,
        option: String,
        content: Content
    ) {
        when(option) {
            options[0] -> {
                copyToClipboard(requireContext(), viewModel.book, content)
                dialogFragment.dismiss()
            }
            options[1] -> {
                share(requireContext(), viewModel.book, content)
                dialogFragment.dismiss()
            }
            options[2] -> {
                shareAsImage(requireContext(), viewModel.book, content)
                dialogFragment.dismiss()
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.bible.views"

        private object Key {
            private const val OBJECT_NAME = "Key"
            const val BOOK = "$PACKAGE_NAME.$OBJECT_NAME.BOOK"
            const val CHAPTER = "$PACKAGE_NAME.$OBJECT_NAME.CHAPTER"
        }

        fun newInstance(book: Int, chapter: Int): ChapterFragment {
            return ChapterFragment().apply {
                arguments = Bundle().apply {
                    putInt(Key.BOOK, book)
                    putInt(Key.CHAPTER, chapter)
                }
            }
        }
    }
}
package com.duke.orca.android.kotlin.biblelockscreen.bible.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.BLANK
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.ONE_SECOND
import com.duke.orca.android.kotlin.biblelockscreen.application.fadeIn
import com.duke.orca.android.kotlin.biblelockscreen.application.fadeOut
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.adapters.VerseAdapter
import com.duke.orca.android.kotlin.biblelockscreen.bible.copyToClipboard
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.share
import com.duke.orca.android.kotlin.biblelockscreen.bible.shareAsImage
import com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels.BibleVerseSearchViewModel
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>(),
    VerseAdapter.OnIconClickListener,
    OptionChoiceDialogFragment.OnOptionChoiceListener {
    override val toolbar: Toolbar? = null

    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSearchBinding {
        return FragmentSearchBinding.inflate(inflater, container, false)
    }

    private val viewModel by viewModels<BibleVerseSearchViewModel>()
    private val verseAdapter by lazy {
        VerseAdapter(viewModel.bibleBook) {
            addFragment(
                R.id.fragment_container_view,
                parentFragmentManager,
                ChapterPagerFragment.newInstance(it.content)
            )
        }
    }

    private val color by lazy { ContextCompat.getColor(requireContext(), R.color.secondary) }
    private val options by lazy { arrayOf(getString(R.string.copy), getString(R.string.share), getString(R.string.share_as_image)) }

    private var currentQuery = BLANK
    private var timerTask: TimerTask? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        observe()
        bind()

        return viewBinding.root
    }

    override fun onResume() {
        super.onResume()
        with(viewBinding.searchView) {
            post {
                isIconified = false
            }
        }
    }

    private fun observe() {
        viewModel.searchResult.observe(viewLifecycleOwner, { searchResult ->
            val searchResults = searchResult.searchResults
            val searchWord = searchResult.searchWord

            viewBinding.circularProgressIndicator.fadeOut(Duration.Animation.FADE_OUT) {
                verseAdapter.submitGroupedList(
                    searchResults.map { it.toAdapterItem() }, searchWord, color
                ) {
                    delayOnLifecycle(Duration.Delay.DISMISS) {
                        if (searchResults.isEmpty()) {
                            viewBinding.linearLayout.fadeIn(Duration.Animation.FADE_IN)
                        } else {
                            viewBinding.recyclerView.fadeIn(Duration.Animation.FADE_IN)
                        }
                    }
                }
            }
        })
    }

    private fun bind() {
        verseAdapter.setOnIconClickListener(this)

        // val searchMagIcon = viewBinding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        // val searchCloseBtn = viewBinding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val searchSrcText = viewBinding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)

        searchSrcText?.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.hint_text))
        searchSrcText?.setTextAppearance(R.style.EditTextStyle)

        viewBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                timerTask?.cancel()

                query?.let {
                    if (it.isNotBlank()) {
                        search(it)
                        viewBinding.searchView.clearFocus()
                    }
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                timerTask?.cancel()

                timerTask = object : TimerTask() {
                    override fun run() {
                        if (newText?.isNotBlank() == true) {
                            if (newText.length > 1) {
                                search(newText)
                            }
                        }
                    }
                }

                Timer().schedule(timerTask, ONE_SECOND)

                return true
            }
        })

        viewBinding.recyclerView.apply {
            adapter = verseAdapter
            layoutManager = LinearLayoutManagerWrapper(context)
            setHasFixedSize(true)
        }
    }

    private fun search(text: String) {
        if (currentQuery == text) return

        currentQuery = text

        lifecycleScope.launch {
            if (viewBinding.linearLayout.isVisible) {
                viewBinding.linearLayout.fadeOut(Duration.Animation.FADE_OUT) {
                    viewBinding.circularProgressIndicator.fadeIn(Duration.Animation.FADE_IN)
                    verseAdapter.submitList(null)
                    viewModel.search(text)
                }
            } else {
                viewBinding.recyclerView.fadeOut(Duration.Animation.FADE_OUT, true) {
                    viewBinding.circularProgressIndicator.fadeIn(Duration.Animation.FADE_IN)
                    verseAdapter.submitList(null)
                    viewModel.search(text)
                }
            }
        }
    }

    override fun onBookmarkClick(id: Int, bookmark: Boolean) {
        viewModel.updateBookmark(id, bookmark)
    }

    override fun onFavoriteClick(id: Int, favorite: Boolean) {
        viewModel.updateFavorite(id, favorite)
    }

    override fun onMoreVertClick(item: VerseAdapter.AdapterItem.Verse) {
        OptionChoiceDialogFragment.newInstance(options, item.content).also {
            it.show(childFragmentManager, it.tag)
        }
    }

    override fun onOptionChoice(
        dialogFragment: DialogFragment,
        option: String,
        content: Content
    ) {
        when(option) {
            options[0] -> {
                copyToClipboard(requireContext(), viewModel.bibleBook, content)
                dialogFragment.dismiss()
            }
            options[1] -> {
                share(requireContext(), viewModel.bibleBook, content)
                dialogFragment.dismiss()
            }
            options[2] -> {
                shareAsImage(requireContext(), viewModel.bibleBook, content)
                dialogFragment.dismiss()
            }
        }
    }

    private fun Verse.toAdapterItem() = VerseAdapter.AdapterItem.Verse(
        id = id,
        book = book,
        chapter = chapter,
        verse = verse,
        word = word,
        bookmark = bookmark,
        favorite = favorite
    )
}
package com.duke.orca.android.kotlin.biblelockscreen.bible.views

import androidx.core.os.BundleCompat
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.*
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.*
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.*
import com.duke.orca.android.kotlin.biblelockscreen.base.viewmodels.FragmentContainerViewModel
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.adapters.ChapterPagerAdapter
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.BookToChapters
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Translation
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Position
import com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels.ChapterPagerViewModel
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentChapterPagerBinding
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.SubDatabase
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.FontSettingsBottomSheetDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.TranslationSelectionDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.widget.DropdownMenu
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ChapterPagerFragment : BaseFragment<FragmentChapterPagerBinding>(),
    BaseDialogFragment.LifecycleCallback,
    BookSelectionDialogFragment.OnBookSelectedListener,
    TranslationSelectionDialogFragment.OnClickListener
{
    override val toolbar: Toolbar by lazy { viewBinding.toolbar }

    private val activityViewModel by activityViewModels<FragmentContainerViewModel>()
    private val viewModel by viewModels<ChapterPagerViewModel>()

    private val fragment: ChapterPagerFragment
        get() = this

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.setCurrentChapter(position.inc())
        }
    }

    private val limit = 2

    private var chapterPagerAdapter: ChapterPagerAdapter? = null
    private var currentBook = 1
    private var currentChapter = 1

    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentChapterPagerBinding {
        return FragmentChapterPagerBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        arguments?.let { BundleCompat.getParcelable(it, Key.CONTENT, Content::class.java) }?.let {
            setup(it.book, it.chapter, it.verse)
        } ?: run {
            runBlocking {
                val recentlyRead = viewModel.recentlyReadDataStore.data.flowOn(Dispatchers.IO).first()

                setup(recentlyRead.book, recentlyRead.chapter, 1)
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
        observe()

        setFragmentResultListener(RequestKey.HIGHLIGHTS_FRAGMENT) { _, bundle ->
            val verse = BundleCompat.getParcelable(bundle, Key.CONTENT, Content::class.java)

            verse?.let {
                clear()
                setup(it.book, it.chapter, it.verse)
            }
        }

        setFragmentResultListener(RequestKey.BOOKMARKS_FRAGMENT) { _, bundle ->
            val verse = BundleCompat.getParcelable(bundle, Key.CONTENT, Content::class.java)

            verse?.let {
                clear()
                setup(it.book, it.chapter, it.verse)
            }
        }

        setFragmentResultListener(RequestKey.FAVORITES_FRAGMENT) { _, bundle ->
            val verse = BundleCompat.getParcelable(bundle, Key.CONTENT, Content::class.java)

            verse?.let {
                clear()
                setup(it.book, it.chapter, it.verse)
            }
        }
    }

    override fun onPause() {
        viewModel.updateRecentlyRead(currentBook, currentChapter)
        super.onPause()
    }

    override fun onBookSelected(
        dialogFragment: BookSelectionDialogFragment,
        item: BookSelectionDialogFragment.AdapterItem.Book
    ) {
        if (currentBook.not(item.book)) {
            clear()
            setup(item.book, 1)
        }

        delayOnLifecycle(Duration.Delay.DISMISS) {
            dialogFragment.dismiss()
        }
    }

    override fun onDialogFragmentViewCreated(tag: String) {
        when(tag){
            BookSelectionDialogFragment.TAG -> {
                delayOnLifecycle(Duration.Delay.ROTATE) {
                    viewBinding.imageViewBook.rotate(180.0f, Duration.Animation.ROTATION)
                }
            }
            TranslationSelectionDialogFragment.TAG -> {
                delayOnLifecycle(Duration.Delay.ROTATE) {
                    viewBinding.imageViewTranslation.rotate(180.0f, Duration.Animation.ROTATION)
                }
            }
        }
    }

    override fun onDialogFragmentViewDestroyed(tag: String) {
        when(tag) {
            BookSelectionDialogFragment.TAG -> {
                delayOnLifecycle(Duration.Delay.ROTATE) {
                    viewBinding.imageViewBook.rotate(0.0f, Duration.Animation.ROTATION)
                }
            }
            TranslationSelectionDialogFragment.TAG -> {
                delayOnLifecycle(Duration.Delay.ROTATE) {
                    viewBinding.imageViewTranslation.rotate(0.0f, Duration.Animation.ROTATION)
                }
            }
        }
    }

    override fun onNegativeButtonClick(dialogFragment: DialogFragment) {
        dialogFragment.dismiss()
    }

    override fun onPositiveButtonClick(
        dialogFragment: DialogFragment,
        translation: Translation.Model,
        subTranslation: Translation.Model?,
        isTranslationChanged: Boolean,
        isSubTranslationChanged: Boolean
    ) {
        if (isTranslationChanged) {
            DataStore.Translation.putFileName(requireContext(), translation.fileName)
        }

        if (isSubTranslationChanged) {
            subTranslation?.let {
                DataStore.Translation.putSubFileName(requireContext(), it.fileName)
            } ?: run {
                DataStore.Translation.putSubFileName(requireContext(), BLANK)
            }
        }

        if (isTranslationChanged || isSubTranslationChanged) {
            activityViewModel.setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RECREATE, true))
            clear()

            if (isTranslationChanged) {
                Database.refresh(requireContext())
            }

            if (isSubTranslationChanged) {
                SubDatabase.refresh(requireContext())
            }
        }

        delayOnLifecycle(Duration.Delay.DISMISS) {
            if (isTranslationChanged || isSubTranslationChanged) {
                onTranslationChanged()
            }

            dialogFragment.dismiss()
        }
    }

    private fun clear() {
        viewBinding.viewPager2.adapter = null
        chapterPagerAdapter = null
    }

    private fun setup(book: Int, chapter: Int, verse: Int = -1) {
        with(viewBinding) {
            val fileName = DataStore.Translation.getFileName(requireContext())
            val subFileName = DataStore.Translation.getSubFileName(requireContext())

            textViewTranslation.text = Translation.findNameByFileName(fileName)

            if (subFileName.isNotBlank()) {
                textViewSubTranslation.show()
                textViewSubTranslation.text = Translation.findNameByFileName(subFileName)
            } else {
                textViewSubTranslation.hide()
                textViewSubTranslation.text = BLANK
            }

            textViewBook.text = bible.name(book)

            val chapters =  BookToChapters.findChaptersByBookId(book).toStringArray()

            dropdownMenuChapter.setAdapter(DropdownMenu.ArrayAdapter(chapters))
            chapterPagerAdapter = ChapterPagerAdapter(fragment, book)

            viewModel.insertPosition(Position(book, chapter, verse.dec()))

            viewPager2.apply {
                adapter = chapterPagerAdapter
                offscreenPageLimit = limit
                registerOnPageChangeCallback(onPageChangeCallback)
                setCurrentItem(chapter.dec(), false)
            }
        }

        currentBook = book
    }

    private fun observe() {
        viewModel.currentChapter.observe(viewLifecycleOwner, { chapter ->
            with(viewBinding) {
                dropdownMenuChapter.setText("$chapter")
            }

            currentChapter = chapter
        })
    }

    private fun bind() {
        with(viewBinding) {
            toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            constraintLayoutTranslation.setOnClickListener {
                TranslationSelectionDialogFragment().also {
                    it.show(childFragmentManager, it.tag)
                }
            }

            imageViewTranslation.setOnClickListener {
                constraintLayoutTranslation.performClick()
            }

            linearLayoutBook.setOnClickListener {
                viewBinding.imageViewBook.performClick()
            }

            imageViewBook.setOnClickListener {
                BookSelectionDialogFragment.newInstance(
                    currentBook
                ).also {
                    it.show(childFragmentManager, it.tag)
                }
            }

            dropdownMenuChapter.setOnItemClickListener { position, _ ->
                viewPager2.setCurrentItem(position, false)
            }

            imageViewHighlight.setOnClickListener {
                addFragment(HighlightsFragment.newInstance(true))
            }

            imageViewBookmarks.setOnClickListener {
                addFragment(BookmarksFragment.newInstance(true))
            }

            imageViewFavorites.setOnClickListener {
                addFragment(FavoritesFragment.newInstance(true))
            }

            imageViewFont.setOnClickListener {
                FontSettingsBottomSheetDialogFragment().also {
                    it.show(childFragmentManager, it.tag)
                }
            }
        }

        val isDarkMode = DataStore.Display.isDarkMode(requireContext())

        if (isDarkMode) {
            viewBinding.imageViewDarkMode.setImageResource(R.drawable.ic_round_light_mode_24)
        } else {
            viewBinding.imageViewDarkMode.setImageResource(R.drawable.ic_round_dark_mode_24)
        }

        viewBinding.imageViewDarkMode.setOnClickListener {
            DataStore.Display.putDarkMode(it.context, isDarkMode.not())
            viewModel.updateRecentlyRead(currentBook, currentChapter)

            delayOnLifecycle(Duration.Delay.RECREATE) {
                activity?.recreate()
            }
        }
    }

    private fun addFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .setReorderingAllowed(true)
            .add(R.id.fragment_container_view, fragment, fragment.tag)
            .addToBackStack(fragment.tag)
            .commit()
    }

    private fun onTranslationChanged() {
        setup(currentBook, currentChapter)
    }

    companion object {
        fun newInstance(content: Content): ChapterPagerFragment {
            return ChapterPagerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Key.CONTENT, content)
                }
            }
        }
    }
}
package com.duke.orca.android.kotlin.biblelockscreen.bible.views

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat.END
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.*
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.*
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseDialogFragment
import com.duke.orca.android.kotlin.biblelockscreen.base.views.BaseFragment
import com.duke.orca.android.kotlin.biblelockscreen.base.views.FragmentContainerActivity
import com.duke.orca.android.kotlin.biblelockscreen.bible.adapters.VersePagerAdapter
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.BookToChapters
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Verse
import com.duke.orca.android.kotlin.biblelockscreen.bible.pagetransformer.PageTransformer
import com.duke.orca.android.kotlin.biblelockscreen.bible.unlock.UnlockController
import com.duke.orca.android.kotlin.biblelockscreen.bible.viewmodels.VersePagerViewModel
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentVersePagerBinding
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.devicecredential.DeviceCredential
import com.duke.orca.android.kotlin.biblelockscreen.devicecredential.annotation.RequireDeviceCredential
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import com.duke.orca.android.kotlin.biblelockscreen.review.Review
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.FontSettingsFragment
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.LockScreenSettingsFragment
import com.duke.orca.android.kotlin.biblelockscreen.settings.views.SettingsFragment
import com.duke.orca.android.kotlin.biblelockscreen.widget.DropdownMenu
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

@AndroidEntryPoint
@RequireDeviceCredential
class VersePagerFragment : BaseFragment<FragmentVersePagerBinding>(),
    BaseDialogFragment.LifecycleCallback,
    BookSelectionDialogFragment.OnBookSelectedListener,
    NavigationView.OnNavigationItemSelectedListener {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentVersePagerBinding {
        return FragmentVersePagerBinding.inflate(inflater, container, false)
    }

    private val viewModel by viewModels<VersePagerViewModel>()
    private val drawerLayout by lazy { viewBinding.drawerLayout }
    private val versePagerAdapter by lazy { VersePagerAdapter(this) }
    private val unlockController by lazy {
        UnlockController(viewBinding, object : UnlockController.Callback {
            override fun onOutOfRanged() {
                if (DeviceCredential.requireUnlock(requireContext())) {
                    confirmDeviceCredential()
                } else {
                    requireActivity().finish()
                }
            }
        })
    }

    private val limit = 2

    private var currentVerse: Verse? = null
    private var currentPageMargin = 0.0f

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.get(position)
            updateArrows(position)
        }
    }

    private val onBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(END)) {
                    drawerLayout.closeDrawer(END)
                } else {
                    if (childFragmentManager.backStackEntryCount > 0) {
                        childFragmentManager.popBackStackImmediate()
                    } else {
                        val unlockWithBackKey = DataStore.LockScreen.getUnlockWithBackKey(requireContext())

                        if (unlockWithBackKey) {
                            finish()
                        } else {
                            viewBinding.frameLayoutUnlock.animateRipple(Duration.SHORT)
                        }
                    }
                }
            }
        }
    }

    private val onDrawListener = ViewTreeObserver.OnDrawListener {
        try {
            with(viewBinding.viewPreviousFake) {
                if (abs(translationX) == 0.0f) {
                    setPageTransformer(PageMargin.small, false)
                    hide(true)
                    viewBinding.viewNextFake.hide(true)
                } else {
                    if (currentVerse?.id.isNonZero()) {
                        show()
                    }

                    if (currentVerse?.id.not(VERSE_COUNT.dec())) {
                        viewBinding.viewNextFake.show()
                    }

                    setPageTransformer(PageMargin.medium, false)
                }
            }
        } catch (e: NullPointerException) {
            Timber.e(e)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        putActivityResultLauncher(Key.FRAGMENT_CONTAINER) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                activityResult.data?.let { data ->
                    if (data.getBooleanExtra(EXTRA_RECREATE, false)) {
                        recreate()
                    }
                }
            }
        }

        putActivityResultLauncher(Key.CHAPTER_PAGER) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                activityResult.data?.let { data ->
                    if (data.getBooleanExtra(EXTRA_RECREATE, false)) {
                        recreate()
                    }
                }
            }
        }

        putBehaviourSubject(Key.DROPDOWN_MENU, BehaviorSubject.create())
        putBehaviourSubject(Key.VIEW_PAGER2, BehaviorSubject.create())

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribe()
        bind()
    }

    override fun onStop() {
        if (viewBinding.drawerLayout.isDrawerOpen(END)) {
            viewBinding.drawerLayout.closeDrawer(END, false)
        }

        super.onStop()
    }

    override fun onDestroyView() {
        DataStore.Verse.putCurrentVerse(requireContext(), viewBinding.viewPager2.currentItem)
        viewBinding.viewPreviousFake.viewTreeObserver.removeOnDrawListener(onDrawListener)
        viewBinding.viewPager2.unregisterOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.adapter = null
        super.onDestroyView()
    }

    override fun onDetach() {
        onBackPressedCallback.remove()
        super.onDetach()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.item_search -> launchFragmentContainerActivity(SearchFragment::class.java.simpleName)
            R.id.item_favorites -> launchFragmentContainerActivity(FavoritesFragment::class.java.simpleName)
            R.id.item_bible -> launchFragmentContainerActivity(ChapterPagerFragment::class.java.simpleName)
            R.id.item_settings -> launchFragmentContainerActivity(SettingsFragment::class.java.simpleName)
            R.id.item_lock_screen -> launchFragmentContainerActivity(LockScreenSettingsFragment::class.java.simpleName)
            R.id.item_font -> launchFragmentContainerActivity(FontSettingsFragment::class.java.simpleName)
            R.id.item_share_the_app -> shareApplication(requireContext())
            R.id.item_write_review -> Review.launchReviewFlow(requireActivity())
        }

        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bind() {
        unlockController.init()

        viewBinding.navigationView.setNavigationItemSelectedListener(this)

        viewBinding.imageViewBible.setOnClickListener {
            launchFragmentContainerActivity(ChapterPagerFragment::class.java.simpleName)
        }

        viewBinding.imageViewSearch.setOnClickListener {
            launchFragmentContainerActivity(SearchFragment::class.java.simpleName)
        }

        viewBinding.imageViewFavorite.setOnClickListener {
            launchFragmentContainerActivity(FavoritesFragment::class.java.simpleName)
        }

        viewBinding.imageViewMenu.setOnClickListener {
            with(drawerLayout) {
                if (isDrawerVisible(END).not()) {
                    openDrawer(END)
                }
            }
        }

        viewBinding.linearLayoutBook.setOnClickListener {
            BookSelectionDialogFragment.newInstance(
                currentVerse?.book ?: 0
            ).also {
                it.show(childFragmentManager, it.tag)
            }
        }

        viewBinding.imageViewBook.setOnClickListener {
            viewBinding.linearLayoutBook.performClick()
        }

        viewBinding.dropdownMenuChapter.setOnItemClickListener { position, _ ->
            val book = currentVerse?.book ?: 1

            moveTo(book, position.inc(), 1)
        }

        viewBinding.dropdownMenuVerse.setOnItemClickListener { position, _ ->
            val book = currentVerse?.book ?: 1
            val chapter = currentVerse?.chapter ?: 1

            moveTo(book, chapter, position.inc())
        }

        viewBinding.imageViewPrevious.setOnClickListener {
            with(viewBinding.viewPager2) { setCurrentItem(currentItem.dec(), true) }
        }

        viewBinding.imageViewNext.setOnClickListener {
            with(viewBinding.viewPager2) { setCurrentItem(currentItem.inc(), true) }
        }

        setPageTransformer(PageMargin.medium, false)

        viewBinding.viewPager2.adapter = versePagerAdapter
        viewBinding.viewPager2.offscreenPageLimit = limit
        viewBinding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.setCurrentItem(DataStore.Verse.getCurrentVerse(requireContext()), false)

        getBehaviourSubject(Key.VIEW_PAGER2)?.onNext(Unit)
    }

    private fun subscribe() {
        ifLet(
            getBehaviourSubject(Key.DROPDOWN_MENU),
            getBehaviourSubject(Key.VIEW_PAGER2)
        ) {
            compositeDisposable.add(Observable.zip(it[0], it[1]) { _, _ ->
            }.subscribe ({
                viewBinding.constraintLayoutContent.fadeIn(Duration.Animation.FADE_IN) {
                    setPageTransformer(PageMargin.small, true) {
                        viewBinding.viewPreviousFake.viewTreeObserver.addOnDrawListener(onDrawListener)
                    }
                }
            }) { t ->
                Timber.e(t)
            })
        }

        compositeDisposable.add(viewModel.behaviorSubject
            .ofType(Verse::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe {
                if (currentVerse?.book.not(it.book)) {
                    viewBinding.textViewBook.text = viewModel.bibleBook.name(it.book)

                    viewBinding.dropdownMenuChapter.setAdapter(
                        DropdownMenu.ArrayAdapter(
                            BookToChapters.findChaptersByBookId(it.book).toStringArray()
                        ), it.chapter.dec()
                    )
                }

                if (currentVerse?.chapter.not(it.chapter)) {
                    viewBinding.dropdownMenuChapter.setText("${it.chapter}")
                }

                if (currentVerse?.verse.not(it.verse)) {
                    viewBinding.dropdownMenuVerse.setText("${it.verse}")
                }

                if (currentVerse?.book.not(it.book) or currentVerse?.chapter.not(it.chapter)) {
                    lifecycleScope.launch {
                        val itemCount = viewModel.getVerseCount(it.book, it.chapter)

                        viewBinding.dropdownMenuVerse.setAdapter(
                            DropdownMenu.ArrayAdapter(
                                intRange(1, itemCount).toStringArray()
                            ), it.verse.dec()
                        )
                    }
                }

                currentVerse = it

                getBehaviourSubject(Key.DROPDOWN_MENU)?.onNext(Unit)
            }
        )
    }

    private fun launchFragmentContainerActivity(simpleName: String) {
        Intent(requireContext(), FragmentContainerActivity::class.java).also {
            it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            it.putExtra(EXTRA_SIMPLE_NAME, simpleName)
            getActivityResultLauncher(Key.FRAGMENT_CONTAINER)?.launch(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.z_adjustment_bottom)
        }
    }

    // 첫 구절·마지막 구절에서는 더 갈 곳이 없는 쪽 화살표를 숨긴다.
    private fun updateArrows(position: Int) {
        viewBinding.imageViewPrevious.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
        viewBinding.imageViewNext.visibility = if (position < VERSE_COUNT.dec()) View.VISIBLE else View.INVISIBLE
    }

    private fun moveTo(book: Int, chapter: Int, verse: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.get(book, chapter, verse)?.let {
                viewBinding.viewPager2.setCurrentItem(it.id, false)
            }
        }
    }

    private fun confirmDeviceCredential() {
        DeviceCredential.confirmDeviceCredential(requireActivity(), object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissCancelled() {
                super.onDismissCancelled()
                unlockController.restore()
            }

            override fun onDismissError() {
                super.onDismissError()
                unlockController.restore()
            }

            override fun onDismissSucceeded() {
                super.onDismissSucceeded()
                requireActivity().finish()
            }
        })
    }

    private fun setPageTransformer(
        pageMargin: Float,
        scheduleAnimation: Boolean,
        onPageAnimationEnd: (() -> Unit)? = null
    ) {
        try {
            if (currentPageMargin == pageMargin) return

            viewBinding.viewPager2.setPageTransformer(PageTransformer(
                pageMargin,
                scheduleAnimation
            ).apply {
                onPageAnimationEnd?.let {
                    setPageAnimatorListener(object : PageTransformer.PageAnimatorListener {
                        override fun onPageAnimationEnd() {
                            it.invoke()
                        }
                    })
                }
            })

            currentPageMargin = pageMargin
        } catch (e: NullPointerException) {
            Timber.e(e)
        }
    }

    override fun onBookSelected(dialogFragment: BookSelectionDialogFragment, item: BookSelectionDialogFragment.AdapterItem.Book) {
        delayOnLifecycle(Duration.Delay.DISMISS) {
            dialogFragment.dismiss()
        }

        moveTo(item.index.inc(), 1, 1)
    }

    override fun onDialogFragmentViewCreated(tag: String) {
        when(tag) {
            BookSelectionDialogFragment.TAG -> {
                delayOnLifecycle(Duration.Delay.ROTATE) {
                    viewBinding.imageViewBook.rotate(180.0f, Duration.Animation.ROTATION)
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
        }
    }

    private fun recreate() {
        viewBinding.viewPreviousFake.viewTreeObserver.removeOnDrawListener(onDrawListener)

        Database.refresh(requireContext())

        findNavController().navigate(
            R.id.bibleVersePagerFragment,
            arguments,
            NavOptions.Builder()
                .setPopUpTo(R.id.bibleVersePagerFragment, true)
                .build()
        )
    }

    companion object {
        private const val PACKAGE_NAME = "${Application.PACKAGE_NAME}.bible.views"

        private object Key {
            const val CHAPTER_PAGER = "$PACKAGE_NAME.CHAPTER_PAGER"
            const val DEVICE_CREDENTIAL = "$PACKAGE_NAME.DEVICE_CREDENTIAL"
            const val DROPDOWN_MENU = "$PACKAGE_NAME.DROPDOWN_MENU"
            const val FRAGMENT_CONTAINER = "$PACKAGE_NAME.FRAGMENT_CONTAINER"
            const val VIEW_PAGER2 = "$PACKAGE_NAME.VIEW_PAGER2"
        }
    }
}
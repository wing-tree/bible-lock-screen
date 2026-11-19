package com.duke.orca.android.kotlin.biblelockscreen.settings.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.*
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.BLANK
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.COMMA
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.application.getVersionName
import com.duke.orca.android.kotlin.biblelockscreen.application.shareApplication
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.viewmodels.FragmentContainerViewModel
import com.duke.orca.android.kotlin.biblelockscreen.base.views.PreferenceFragment
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Translation
import com.duke.orca.android.kotlin.biblelockscreen.dailyverse.DailyVerseNotice
import com.duke.orca.android.kotlin.biblelockscreen.datastore.DataStore
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.SubDatabase
import com.duke.orca.android.kotlin.biblelockscreen.review.Review
import com.duke.orca.android.kotlin.biblelockscreen.settings.adapters.PreferenceAdapter.AdapterItem
import java.lang.StringBuilder

class SettingsFragment : PreferenceFragment(),
    TranslationSelectionDialogFragment.OnClickListener,
    HourChoiceDialogFragment.OnItemClickListener {
    override val toolbar by lazy { viewBinding.toolbar }
    override val toolbarTitleResId: Int = R.string.settings

    private object Id {
        const val TRANSLATION = 0L
        const val DAILY_VERSE_TIME = 1L
    }

    private val activityViewModel by activityViewModels<FragmentContainerViewModel>()

    override fun onNegativeButtonClick(dialogFragment: DialogFragment) {
        delayOnLifecycle(Duration.Delay.DISMISS) {
            dialogFragment.dismiss()
        }
    }

    override fun onHourClick(dialogFragment: DialogFragment, item: Int) {
        DataStore.Notice.putDailyVerseHour(requireContext(), item)
        DailyVerseNotice.schedule(requireContext())
        preferenceAdapter.updateSummary(Id.DAILY_VERSE_TIME, DailyVerseNotice.format(item))
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

            SubDatabase.refresh(requireContext())
        }

        if (isTranslationChanged || isSubTranslationChanged) {
            val stringBuilder = StringBuilder()

            stringBuilder.append(translation.name)

            subTranslation?.let {
                stringBuilder.append("$COMMA ")
                stringBuilder.append(it.name)
            }

            val summary = stringBuilder.toString()

            activityViewModel.setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RECREATE, true))
            preferenceAdapter.updateSummary(Id.TRANSLATION, summary)
        }

        delayOnLifecycle(Duration.Delay.DISMISS) {
            dialogFragment.dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        initData()
        bind()

        return viewBinding.root
    }

    private fun initData() {
        val stringBuilder = StringBuilder()
        val subFileName = DataStore.Translation.getSubFileName(requireContext())

        stringBuilder.append(Translation.getName(requireContext()))

        if (subFileName.isNotBlank()) {
            stringBuilder.append("$COMMA ")
            stringBuilder.append(Translation.findNameByFileName(subFileName))
        }

        val translationSummary = stringBuilder.toString()

        preferenceAdapter.submitList(
            arrayListOf(
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_stay_primary_portrait_24
                    ),
                    summary = getString(R.string.dark_mode),
                    onClick = {
                        addFragment(DisplaySettingsFragment())
                    },
                    body = getString(R.string.display)
                ),
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_screen_lock_portrait_24
                    ),
                    summary = "${getString(R.string.lock_setting)}, ${getString(R.string.lock_type)}",
                    onClick = {
                        addFragment(LockScreenSettingsFragment())
                    },
                    body = getString(R.string.lock_screen)
                ),
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_text_format_24
                    ),
                    onClick = {
                        addFragment(FontSettingsFragment())
                    },
                    summary = "${getString(R.string.bold)}," +
                            " ${getString(R.string.font_size)}," +
                            " ${getString(R.string.text_alignment)}"
                    ,
                    body = getString(R.string.font)
                ),
                AdapterItem.Space(),
                AdapterItem.SwitchPreference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.round_notifications_24
                    ),
                    isChecked = DataStore.Notice.isDailyVerseOn(requireContext()),
                    onCheckedChange = { isChecked ->
                        DataStore.Notice.putDailyVerseOn(requireContext(), isChecked)
                        DailyVerseNotice.schedule(requireContext())
                    },
                    body = getString(R.string.daily_verse_notice)
                ),
                AdapterItem.Preference(
                    id = Id.DAILY_VERSE_TIME,
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.round_schedule_24
                    ),
                    onClick = {
                        HourChoiceDialogFragment().also {
                            it.show(childFragmentManager, it.tag)
                        }
                    },
                    summary = DailyVerseNotice.format(DataStore.Notice.getDailyVerseHour(requireContext())),
                    body = getString(R.string.daily_verse_time)
                ),
                AdapterItem.Space(),
                AdapterItem.Preference(
                    id = Id.TRANSLATION,
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_translate_24
                    ),
                    onClick = {
                        TranslationSelectionDialogFragment().also {
                            it.show(childFragmentManager, it.tag)
                        }
                    },
                    summary = translationSummary,
                    body = getString(R.string.translations)
                ),
                AdapterItem.Space(),
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_share_24
                    ),
                    onClick = {
                        shareApplication(requireContext())
                    },
                    body = getString(R.string.share_the_app),
                    summary = BLANK
                ),
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_rate_review_24
                    ),
                    onClick = {
                        Review.launchReviewFlow(requireActivity())
                    },
                    body = getString(R.string.write_review),
                    summary = BLANK
                ),
                AdapterItem.Preference(
                    drawable = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_round_info_24
                    ),
                    isClickable = false,
                    onClick = {
                    },
                    summary = getVersionName(requireContext()),
                    body = getString(R.string.version)
                )
            )
        )
    }

    private fun bind() {
        viewBinding.recyclerView.apply {
            adapter = preferenceAdapter
            layoutManager = LinearLayoutManagerWrapper(requireContext())
            setHasFixedSize(true)
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
            .add(R.id.fragment_container_view, fragment, fragment.tag)
            .addToBackStack(null)
            .commit()
    }
}
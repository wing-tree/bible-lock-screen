package com.duke.orca.android.kotlin.biblelockscreen.bible.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.VERSE_COUNT
import com.duke.orca.android.kotlin.biblelockscreen.bible.views.VerseFragment

class VersePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = ITEM_COUNT

    override fun createFragment(position: Int): Fragment {
        return VerseFragment.newInstance(position)
    }

    companion object {
        private const val ITEM_COUNT = VERSE_COUNT
    }
}
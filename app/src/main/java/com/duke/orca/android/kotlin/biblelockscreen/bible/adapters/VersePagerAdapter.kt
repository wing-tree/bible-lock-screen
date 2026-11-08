package com.duke.orca.android.kotlin.biblelockscreen.bible.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.VERSE_COUNT
import com.duke.orca.android.kotlin.biblelockscreen.bible.views.VerseFragment

// 기본은 성경 전체(자리 = 구절 id). ids 를 주면 그 구절들만 넘긴다(즐겨찾기만 보기).
class VersePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    var ids: IntArray? = null

    override fun getItemCount(): Int = ids?.size ?: VERSE_COUNT

    override fun createFragment(position: Int): Fragment {
        return VerseFragment.newInstance(idAt(position))
    }

    override fun getItemId(position: Int): Long = idAt(position).toLong()

    override fun containsItem(itemId: Long): Boolean {
        return ids?.contains(itemId.toInt()) ?: (itemId in 0 until VERSE_COUNT)
    }

    fun idAt(position: Int): Int = ids?.get(position) ?: position

    // 이 목록에 없는 구절이면 -1.
    fun positionOf(id: Int): Int = ids?.indexOf(id) ?: id
}

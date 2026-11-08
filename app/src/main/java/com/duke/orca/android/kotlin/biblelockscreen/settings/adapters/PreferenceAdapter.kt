package com.duke.orca.android.kotlin.biblelockscreen.settings.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.*
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.databinding.*

class PreferenceAdapter: ListAdapter<PreferenceAdapter.AdapterItem, PreferenceAdapter.ViewHolder>(DiffCallback()) {
    private var recyclerView: RecyclerView? = null

    inner class ViewHolder(private val viewBinding: ViewBinding): RecyclerView.ViewHolder(viewBinding.root) {
        private val duration = Duration.MEDIUM

        fun bind(adapterItem: AdapterItem) {
            viewBinding.root.isClickable = adapterItem.isClickable

            when(viewBinding) {
                is PreferenceBinding -> {
                    if (adapterItem is AdapterItem.Preference) {
                        adapterItem.drawable?.let {
                            viewBinding.imageViewIcon.show()
                            viewBinding.imageViewIcon.setImageDrawable(it)
                        } ?: let {
                            viewBinding.imageViewIcon.hide()
                        }

                        viewBinding.textViewBody.text = adapterItem.body
                        viewBinding.textViewSummary.text = adapterItem.summary

                        if (adapterItem.summary.isBlank()) {
                            viewBinding.textViewSummary.hide()
                        }

                        viewBinding.root.setOnClickListener {
                            adapterItem.onClick.invoke(adapterItem)
                        }

                        if (isAboveSpace(bindingAdapterPosition)) {
                            viewBinding.viewDivider.hide()
                        } else {
                            viewBinding.viewDivider.show()
                        }
                    }
                }
                is MultiSelectListPreferenceBinding -> {
                    if (adapterItem is AdapterItem.MultiSelectListPreference) {
                        viewBinding.recyclerViewEntries.apply {
                            adapter = adapterItem.adapter
                            layoutManager = LinearLayoutManagerWrapper(viewBinding.root.context)
                            setHasFixedSize(true)

                            addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                                    if (e.action == MotionEvent.ACTION_MOVE)
                                        rv.parent.requestDisallowInterceptTouchEvent(true)

                                    return false
                                }

                                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                            })
                        }

                        viewBinding.textViewBody.text = adapterItem.body

                        viewBinding.root.setOnClickListener {
                            if (adapterItem.isExpanded) {
                                val context = viewBinding.root.context
                                val constrainedHeight = context.resources.getDimensionPixelSize(R.dimen.height_256dp)
                                val heightOneLine = context.resources.getDimensionPixelSize(R.dimen.height_48dp)

                                var to = adapterItem.adapter.itemCount * heightOneLine

                                if (to > constrainedHeight)
                                    to = constrainedHeight

                                viewBinding.imageViewKeyboardArrowUp.rotate(0.0f, duration)
                                viewBinding.constraintLayoutEntries.expand(duration, to.inc())
                            } else {
                                viewBinding.imageViewKeyboardArrowUp.rotate(180.0f, duration)
                                viewBinding.constraintLayoutEntries.collapse(duration, 0)
                            }

                            adapterItem.isExpanded = adapterItem.isExpanded.not()
                            adapterItem.onClick.invoke(adapterItem)
                        }
                    }
                }
                is PreferenceCategoryBinding -> {
                    if (adapterItem is AdapterItem.PreferenceCategory) {
                        viewBinding.textViewCategory.text = adapterItem.category
                    }
                }
                is SpaceBinding -> {
                }
                is SwitchPreferenceBinding -> {
                    if (adapterItem is AdapterItem.SwitchPreference) {
                        viewBinding.imageViewIcon.setImageDrawable(adapterItem.drawable)
                        viewBinding.switchMaterial.isChecked = adapterItem.isChecked
                        viewBinding.textViewBody.text = adapterItem.body

                        adapterItem.drawable?.let {
                            viewBinding.imageViewIcon.show()
                            viewBinding.imageViewIcon.setImageDrawable(it)
                            viewBinding.viewPadding.show()
                        } ?: let {
                            viewBinding.imageViewIcon.hide()
                            viewBinding.viewPadding.hide()
                        }

                        viewBinding.switchMaterial.setOnCheckedChangeListener { _, isChecked ->
                            adapterItem.onCheckedChange(isChecked)
                        }

                        viewBinding.root.setOnClickListener {
                            viewBinding.switchMaterial.toggle()
                        }
                    }
                }
            }
        }

        fun updateSummary(summary: String) {
            if (viewBinding is PreferenceBinding) {
                viewBinding.textViewSummary.text = summary
            }
        }

        fun updateDrawable(drawable: Drawable?) {
            if (viewBinding is PreferenceBinding) {
                viewBinding.imageViewIcon.setImageDrawable(drawable)
            }
        }
    }

    private fun createViewHolder(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewHolder {
        val viewBinding = when(viewType) {
            ViewType.Preference -> PreferenceBinding.inflate(layoutInflater, parent, false)
            ViewType.MultiSelectListPreference -> MultiSelectListPreferenceBinding.inflate(layoutInflater, parent, false)
            ViewType.PreferenceCategory -> PreferenceCategoryBinding.inflate(layoutInflater, parent, false)
            ViewType.Space -> SpaceBinding.inflate(layoutInflater, parent, false)
            ViewType.SwitchPreference -> SwitchPreferenceBinding.inflate(layoutInflater, parent, false)
            else -> throw IllegalArgumentException("Invalid viewType")
        }

        return ViewHolder(viewBinding)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(LayoutInflater.from(parent.context), parent, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount(): Int = currentList.count()

    override fun getItemViewType(position: Int): Int {
        return when(currentList[position]) {
            is AdapterItem.MultiSelectListPreference -> ViewType.MultiSelectListPreference
            is AdapterItem.Preference -> ViewType.Preference
            is AdapterItem.PreferenceCategory -> ViewType.PreferenceCategory
            is AdapterItem.Space -> ViewType.Space
            is AdapterItem.SwitchPreference -> ViewType.SwitchPreference
        }
    }

    fun getItem(id: Long) = currentList.find { it.id == id }

    fun getPosition(id: Long) = currentList.indexOf(getItem(id))

    fun updateDrawable(id: Long, drawable: Drawable?) {
        val item = currentList.find { it.id == id } ?: return
        val index = currentList.indexOf(item)

        if (item is AdapterItem.Preference) {
            val viewHolder = recyclerView?.findViewHolderForAdapterPosition(index)

            if (viewHolder is ViewHolder) {
                viewHolder.updateDrawable(drawable)
            }
        }
    }

    fun updateSummary(id: Long, summary: String) {
        val item = currentList.find { it.id == id } ?: return
        val index = currentList.indexOf(item)

        when(item) {
            is AdapterItem.Preference -> {
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(index)

                if (viewHolder is ViewHolder) {
                    viewHolder.updateSummary(summary)
                }
            }
            else -> return
        }
    }

    private fun isAboveSpace(position: Int): Boolean {
        return if (position < currentList.count().dec()) {
            currentList[position.inc()] is AdapterItem.Space
        } else
            false
    }

    private object ViewType {
        const val MultiSelectListPreference = 0
        const val Preference = 1
        const val PreferenceCategory = 2
        const val Space = 3
        const val SwitchPreference = 4
    }

    sealed class AdapterItem {
        abstract val id: Long
        abstract var isClickable: Boolean

        data class MultiSelectListPreference(
            override val id: Long = -1L,
            override var isClickable: Boolean = true,
            val adapter: RecyclerView.Adapter<*>,
            val body: String,
            val drawable: Drawable?,
            val onClick: (MultiSelectListPreference) -> Unit,
            var isExpanded: Boolean = false
        ) : AdapterItem()

        data class Preference(
            override val id: Long = -1L,
            override var isClickable: Boolean = true,
            val body: String,
            val drawable: Drawable?,
            val onClick: (Preference) -> Unit,
            val summary: String
        ) : AdapterItem()

        data class PreferenceCategory(
            override val id: Long = -1L,
            override var isClickable: Boolean = true,
            val category: String
        ) : AdapterItem()

        data class Space(
            override val id: Long = -1L,
            override var isClickable: Boolean = false,
        ) : AdapterItem()

        data class SwitchPreference(
            override val id: Long = -1L,
            override var isClickable: Boolean = true,
            val body: String,
            val drawable: Drawable?,
            val isChecked: Boolean,
            val onCheckedChange: (isChecked: Boolean) -> Unit
        ) : AdapterItem()
    }

    class DiffCallback: DiffUtil.ItemCallback<AdapterItem>() {
        override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
            return oldItem == newItem
        }
    }
}
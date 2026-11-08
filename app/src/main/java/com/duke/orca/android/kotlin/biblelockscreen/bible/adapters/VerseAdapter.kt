package com.duke.orca.android.kotlin.biblelockscreen.bible.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewbinding.ViewBinding
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.BLANK
import com.duke.orca.android.kotlin.biblelockscreen.application.setTextWithSearchWord
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Bible
import com.duke.orca.android.kotlin.biblelockscreen.databinding.BookItemBinding
import com.duke.orca.android.kotlin.biblelockscreen.databinding.VerseItemBinding
import com.like.LikeButton
import com.like.OnLikeListener

class VerseAdapter(
    private val bible: Bible,
    private val onItemClick: ((item: AdapterItem.Verse) -> Unit)? = null
) : ListAdapter<VerseAdapter.AdapterItem, VerseAdapter.ViewHolder>(DiffCallback()) {
    private var inflater: LayoutInflater? = null
    private var onIconClickListener: OnIconClickListener? = null
    private var recyclerView: RecyclerView? = null

    private var searchWord = BLANK
    @ColorInt
    private var color = 0

    interface OnIconClickListener {
        fun onBookmarkClick(id: Int, bookmark: Boolean)
        fun onFavoriteClick(id: Int, favorite: Boolean)
        fun onMoreVertClick(item: AdapterItem.Verse)
    }

    fun setOnIconClickListener(onIconClickListener: OnIconClickListener) {
        this.onIconClickListener = onIconClickListener
    }

    fun submitGroupedList(list: List<AdapterItem>, commitCallback: (() -> Unit)? = null) {
        val arrayList = arrayListOf<AdapterItem>()
        val map = with(list.filterIsInstance<AdapterItem.Verse>()) {
            groupBy { AdapterItem.Book(it.book) }
        }

        map.forEach { (book, verses) ->
            arrayList.add(book)
            arrayList.addAll(verses)
        }

        submitList(arrayList) {
            commitCallback?.invoke()
        }
    }

    fun submitGroupedList(
        list: List<AdapterItem>,
        searchWord: String,
        @ColorInt color: Int,
        commitCallback: (() -> Unit)? = null
    ) {
        this.searchWord = searchWord
        this.color = color

        submitGroupedList(list) {
            commitCallback?.invoke()
        }
    }

    inner class ViewHolder(private val viewBinding: ViewBinding): RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: AdapterItem) {
            when(item) {
                is AdapterItem.Book -> {
                    if (viewBinding is BookItemBinding) {
                        viewBinding.root.text = bible.name(item.value)
                    }
                }
                is AdapterItem.Verse -> {
                    if (viewBinding is VerseItemBinding) {
                        viewBinding.textViewBook.text = bible.name(item.book)
                        viewBinding.textViewChapter.text = "${item.chapter}"
                        viewBinding.textViewVerse.text = "${item.verse}"

                        if (searchWord.isBlank()) {
                            viewBinding.textViewWord.text = item.word
                        } else {
                            viewBinding.textViewWord.setTextWithSearchWord(
                                item.word,
                                searchWord,
                                color
                            )
                        }

                        viewBinding.likeButtonBookmark.isLiked = item.bookmark
                        viewBinding.likeButtonBookmark.setOnLikeListener(object : OnLikeListener {
                            override fun liked(likeButton: LikeButton?) {
                                onIconClickListener?.onBookmarkClick(item.id, true)
                            }

                            override fun unLiked(likeButton: LikeButton?) {
                                onIconClickListener?.onBookmarkClick(item.id, true)
                            }
                        })

                        viewBinding.likeButtonFavorite.isLiked = item.favorite
                        viewBinding.likeButtonFavorite.setOnLikeListener(object : OnLikeListener {
                            override fun liked(likeButton: LikeButton?) {
                                onIconClickListener?.onFavoriteClick(item.id, true)
                            }

                            override fun unLiked(likeButton: LikeButton?) {
                                onIconClickListener?.onFavoriteClick(item.id, false)
                            }
                        })

                        viewBinding.imageViewMoreVert.setOnClickListener {
                            onIconClickListener?.onMoreVertClick(item)
                        }

                        onItemClick?.let { onItemClick ->
                            viewBinding.root.setOnClickListener {
                                onItemClick(item)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView

        recyclerView.itemAnimator.also {
            if (it is SimpleItemAnimator) {
                it.supportsChangeAnimations = false
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is AdapterItem.Book -> ViewType.BOOK
            is AdapterItem.Verse -> ViewType.VERSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = this.inflater ?: LayoutInflater.from(parent.context)
        this.inflater = inflater

        val viewBinding = when(viewType) {
            ViewType.BOOK -> BookItemBinding.inflate(inflater, parent, false)
            ViewType.VERSE -> VerseItemBinding.inflate(inflater, parent, false)
            else -> throw IllegalArgumentException("IllegalArgumentException :$viewType")
        }

        return ViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private object ViewType {
            const val BOOK = 0
            const val VERSE = 1
        }
    }

    class DiffCallback: DiffUtil.ItemCallback<AdapterItem>() {
        override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
            return oldItem == newItem
        }
    }

    sealed class AdapterItem {
        abstract val id: Int

        data class Book(val value: Int) : AdapterItem() {
            override val id = -1
        }

        data class Verse(
            override val id: Int,
            val book: Int,
            val chapter: Int,
            val verse: Int,
            val word: String,
            val bookmark: Boolean,
            val favorite: Boolean
        ) : AdapterItem() {
            val content: Content
                get() = Content(
                    book = book,
                    chapter = chapter,
                    verse = verse,
                    word = word
                )
        }
    }
}
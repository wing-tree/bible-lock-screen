package com.duke.orca.android.kotlin.biblelockscreen.bible.adapters

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Parcelable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.*
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.bible.hyphenated
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Font
import com.duke.orca.android.kotlin.biblelockscreen.databinding.OptionsMenuBarBinding
import com.duke.orca.android.kotlin.biblelockscreen.databinding.WordItemBinding
import com.like.LikeButton
import com.like.OnLikeListener
import kotlinx.parcelize.Parcelize

class WordAdapter(context: Context) : ListAdapter<WordAdapter.AdapterItem, WordAdapter.ViewHolder>(DiffCallback()) {
    private val layoutInflater = LayoutInflater.from(context)
    private val highlightColors = context.resources.getIntArray(R.array.highlight_colors)

    private var currentFocus: Int = -1
    private var currentFont: Font? = null
    private var recyclerView: RecyclerView? = null
    private var onOptionsItemSelectedListener: OnOptionsItemSelectedListener? = null

    val currentFocusedItem: AdapterItem
        get() = getItem(currentFocus)

    interface OnOptionsItemSelectedListener {
        fun onOptionsItemSelected(item: AdapterItem.Word, optionsItem: OptionsItem)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(WordItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    // 형광펜 아래 띠는 그 색으로 칠한다. 펜(0번 층) 색은 bind 에서 정한다.
    private fun highlighter(context: Context, @ColorInt color: Int): LayerDrawable {
        fun layer(@DrawableRes id: Int, @ColorInt tint: Int) =
            ContextCompat.getDrawable(context, id)!!.mutate().apply { setTint(tint) }

        return LayerDrawable(arrayOf(
            layer(R.drawable.ic_ink_highlighter_pen_28, ContextCompat.getColor(context, R.color.unhighlighted)),
            layer(R.drawable.ic_ink_highlighter_bar_28, color),
        ))
    }

    private val argbEvaluator = ArgbEvaluator()

    @ColorInt
    private fun blend(@ColorInt from: Int, @ColorInt to: Int, fraction: Float) =
        argbEvaluator.evaluate(fraction, from, to) as Int

    private fun setCurrentFocus(position: Int) {
        currentFocus = if (currentFocus.`is`(position)) {
            -1
        } else {
            notifyItemChanged(currentFocus)
            position
        }

        notifyItemChanged(position)
    }

    fun setFont(font: Font) {
        this.currentFont?.let {
            if(it.contentEquals(font).not()) {
                this.currentFont = font
                notifyItemRangeChanged(0, itemCount)
            }
        } ?: run {
            this.currentFont = font
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun setOnOptionsItemSelectedListener(onOptionsItemSelectedListener: OnOptionsItemSelectedListener) {
        this.onOptionsItemSelectedListener = onOptionsItemSelectedListener
    }

    inner class ViewHolder(private val viewBinding: WordItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        // 하이라이트를 바꾸면 글자 바탕·형광펜·지우개를 페이드로 바꾼다.
        private var boundId = -1
        @ColorInt private var boundColor = 0
        private var fade: ValueAnimator? = null

        fun bind(position: Int) {
            when(val item = getItem(position)) {
                is AdapterItem.Word -> {
                    val verse = item.verse.toString()

                    with(viewBinding) {
                        currentFont?.let {
                            textViewVerse.setTextSize(TypedValue.COMPLEX_UNIT_DIP, it.size)
                            textViewWord.setTextSize(TypedValue.COMPLEX_UNIT_DIP, it.size)
                            textViewWord.gravity = it.textAlignment

                            textViewSubWord.setTextSize(TypedValue.COMPLEX_UNIT_DIP, it.size)
                            textViewSubWord.gravity = it.textAlignment
                        }

                        textViewVerse.text = verse

                        // 같은 구절의 색이 바뀐 것이면 바뀌기 전 색에서부터 페이드한다.
                        val from = if (item.id == boundId) boundColor else item.highlightColor

                        boundId = item.id
                        boundColor = item.highlightColor
                        fade?.cancel()

                        val updates = mutableListOf<(Float) -> Unit>()

                        bindHighlight(item, from)?.let { updates += it }

                        if (item.subWord == null) {
                            textViewSubWord.hide()
                        }

                        if (currentFocus.`is`(position)) {
                            updates += optionsMenuBar.bind(item, from)

                            with(frameLayout) {
                                if (isNotVisible) {
                                    expand(Duration.Animation.EXPAND)
                                }
                            }
                        } else {
                            with(frameLayout) {
                                if (isVisible) {
                                    collapse(Duration.Animation.COLLAPSE)
                                }
                            }
                        }

                        constraintLayout.setOnClickListener {
                            setCurrentFocus(position)
                        }

                        if (updates.isNotEmpty()) {
                            fade = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
                                duration = Duration.MEDIUM
                                addUpdateListener { animator ->
                                    updates.forEach { it(animator.animatedFraction) }
                                }
                                start()
                            }
                        }
                    }
                }
            }
        }

        // 글자 바탕색을 칠한다. 색이 바뀌었으면 페이드할 함수를 돌려준다.
        private fun WordItemBinding.bindHighlight(item: AdapterItem.Word, @ColorInt from: Int): ((Float) -> Unit)? {
            val to = item.highlightColor
            val word = item.word.hyphenated()
            val subWord = item.subWord?.hyphenated()

            if (from == to) {
                textViewWord.setHighlight(to, word)
                subWord?.let { textViewSubWord.setHighlight(to, it) }

                return null
            }

            // 없던 색은 같은 색의 투명에서, 지우는 색은 투명으로 간다.
            val start = if (from.isZero()) to and 0x00FFFFFF else from
            val end = if (to.isZero()) from and 0x00FFFFFF else to
            val span = HighlightColorSpan(start)

            textViewWord.setHighlightedText(span, word)
            subWord?.let { textViewSubWord.setHighlightedText(span, it) }

            return { fraction ->
                span.highlightColor = blend(start, end, fraction)
                textViewWord.invalidate()
                textViewSubWord.invalidate()

                if (fraction >= 1.0f && to.isZero()) {
                    textViewWord.text = word
                    subWord?.let { textViewSubWord.text = it }
                }
            }
        }

        private fun TextView.setHighlight(@ColorInt color: Int, text: String) {
            if (color.isZero()) {
                this.text = text
            } else {
                setHighlightedText(color, text)
            }
        }

        private fun OptionsMenuBarBinding.bind(item: AdapterItem.Word, @ColorInt from: Int): List<(Float) -> Unit> {
            val context = root.context
            val updates = mutableListOf<(Float) -> Unit>()

            textViewVerse.text = item.verse.toString()

            // 형광펜을 누르면 창 없이 바로 칠한다.
            with(linearLayoutHighlightColors) {
                if (childCount == 0) {
                    val size = context.resources.getDimensionPixelSize(R.dimen.width_40dp)
                    val ripple = TypedValue().also {
                        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, it, true)
                    }

                    highlightColors.forEach { color ->
                        addView(ImageView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(size, size)
                            scaleType = ImageView.ScaleType.CENTER
                            setBackgroundResource(ripple.resourceId)
                            setImageDrawable(highlighter(context, color))
                        })
                    }
                }

                // 칠해 둔 색은 펜까지 그 색으로 칠한다.
                val unpicked = ContextCompat.getColor(context, R.color.unhighlighted)

                fun penColor(@ColorInt color: Int, @ColorInt picked: Int) = if (color == picked) color else unpicked

                highlightColors.forEachIndexed { index, color ->
                    with(getChildAt(index) as ImageView) {
                        val pen = (drawable as LayerDrawable).getDrawable(0)
                        val start = penColor(color, from)
                        val end = penColor(color, item.highlightColor)

                        pen.setTint(start)

                        if (start != end) {
                            updates += { pen.setTint(blend(start, end, it)) }
                        }

                        setOnClickListener {
                            onOptionsItemSelectedListener?.onOptionsItemSelected(item, OptionsItem.Highlight(color))
                        }
                    }
                }
            }

            with(imageViewRemoveHighlight) {
                animate().cancel()

                when {
                    from == item.highlightColor -> {
                        alpha = 1.0f
                        isVisible = item.highlightColor.isNonZero()
                    }
                    item.highlightColor.isZero() -> fadeOut(Duration.MEDIUM)
                    from.isZero() -> fadeIn(Duration.MEDIUM)
                    else -> {
                        alpha = 1.0f
                        show()
                    }
                }
            }

            imageViewRemoveHighlight.setOnClickListener {
                onOptionsItemSelectedListener?.onOptionsItemSelected(item, OptionsItem.Highlight(0))
            }

            likeButtonFavorite.isLiked = item.favorite

            likeButtonFavorite.setOnLikeListener(object : OnLikeListener {
                override fun liked(likeButton: LikeButton?) {
                    onOptionsItemSelectedListener?.onOptionsItemSelected(
                        item,
                        OptionsItem.Favorite(true)
                    )
                }

                override fun unLiked(likeButton: LikeButton?) {
                    onOptionsItemSelectedListener?.onOptionsItemSelected(
                        item,
                        OptionsItem.Favorite(false)
                    )
                }
            })

            imageViewMore.setOnClickListener {
                onOptionsItemSelectedListener?.onOptionsItemSelected(item, OptionsItem.More)
            }

            return updates
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

        @Parcelize
        data class Word(
            override val id: Int,
            val book: Book,
            val subBook: Book? = null,
            val chapter: Int,
            val verse: Int,
            val word: String,
            val subWord: String? = null,
            val bookmark: Boolean,
            val favorite: Boolean,
            @ColorInt val highlightColor: Int
        ): AdapterItem(), Parcelable {
            @Parcelize
            data class Book(
                val id: Int,
                val name: String
            ) : Parcelable

            val content: Content
                get() = Content(book.id, chapter, verse, word)
        }
    }

    sealed class OptionsItem {
        class Highlight(@ColorInt val highlightColor: Int) : OptionsItem()
        class Favorite(val liked: Boolean) : OptionsItem()
        object More : OptionsItem()
    }
}
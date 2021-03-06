package com.duke.orca.android.kotlin.biblelockscreen.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.Duration
import com.duke.orca.android.kotlin.biblelockscreen.application.rotate
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.databinding.DropdownItemBinding
import com.duke.orca.android.kotlin.biblelockscreen.databinding.DropdownMenuBinding
import com.duke.orca.android.kotlin.biblelockscreen.databinding.PopupWindowBinding
import kotlinx.coroutines.*
import timber.log.Timber

class DropdownMenu : FrameLayout {
    constructor(context: Context) : super(context) {
        bind()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        bind()
        getAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        bind()
        getAttrs(attrs, defStyleAttr)
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int, item: String)
    }

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onDetachedFromWindow() {
        job.cancel()
        super.onDetachedFromWindow()
    }

    fun setOnItemClickListener(onItemClick: (position: Int, item: String) -> Unit) {
        onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, item: String) {
                coroutineScope.launch {
                    delay(Duration.Delay.DISMISS)
                    popupWindow.dismiss()
                }

                onItemClick(position, item)
            }
        }

        onItemClickListener?.let {
            arrayAdapter?.setOnItemClickListener(it)
        }
    }

    private val layoutInflater by lazy { LayoutInflater.from(context) }
    private val viewBinding by lazy { DropdownMenuBinding.inflate(layoutInflater, this, false) }

    private val popupWindow by lazy {
        PopupWindow().apply {
            setOnDismissListener {
                coroutineScope.launch {
                    delay(Duration.Delay.ROTATE)
                    viewBinding.imageView.rotate(180.0f, 0.0f, Duration.Animation.ROTATION)
                }
            }
        }
    }

    private val popupWindowBinding by lazy { PopupWindowBinding.inflate(layoutInflater) }

    val recyclerView: RecyclerView
        get() = popupWindowBinding.recyclerView

    private val elevationPopupWindow by lazy { resources.getDimension(R.dimen.elevation_6dp) }
    private val itemHeight by lazy { resources.getDimensionPixelOffset(R.dimen.height_48dp) }
    private val marginBottom by lazy { resources.getDimensionPixelOffset(R.dimen.height_8dp) }
    private val marginTop by lazy { resources.getDimensionPixelOffset(R.dimen.height_8dp) }

    private var arrayAdapter: ArrayAdapter? = null
    private var onItemClickListener: OnItemClickListener? = null

    private fun getAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DropdownMenu)

        setTypedArray(typedArray)
    }

    private fun getAttrs(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DropdownMenu, defStyleAttr, 0)

        setTypedArray(typedArray)
    }

    private fun setTypedArray(typedArray: TypedArray) {
        val rippleDrawable = typedArray.getDrawable(R.styleable.DropdownMenu_rippleDrawable)
        val textColor = typedArray.getColor(R.styleable.DropdownMenu_textColor, context.getColor(R.color.text))
        val tint = typedArray.getColor(R.styleable.DropdownMenu_tint, context.getColor(R.color.icon))

        rippleDrawable?.let {
            viewBinding.linearLayout.background = it
        }

        viewBinding.rollingTextView.setTextColor(textColor)
        viewBinding.imageView.setColorFilter(tint)

        typedArray.recycle()
    }

    private fun bind() {
        addView(viewBinding.root)

        viewBinding.root.setOnClickListener {
            showAsDropdown()
        }

        viewBinding.imageView.setOnClickListener {
            showAsDropdown()
        }
    }

    private fun showAsDropdown() {
        if (popupWindow.isShowing.not()){
            popupWindow.contentView = popupWindowBinding.root
            popupWindow.elevation = elevationPopupWindow
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true
            popupWindow.width = width

            val itemCount = arrayAdapter?.itemCount ?: 0
            val height = if (itemCount < 10) {
                itemCount * itemHeight
            } else {
                itemHeight * 9
            }

            popupWindow.height = height + marginBottom + marginTop

            viewBinding.imageView.rotate(0.0f, 180.0f, Duration.Animation.ROTATION)
            popupWindow.showAsDropDown(this)
        }
    }

    fun setAdapter(arrayAdapter: ArrayAdapter, currentItem: Int = 0) {
        setAdapter(arrayAdapter)

        try {
            viewBinding.rollingTextView.currentItem = arrayAdapter.getItem(currentItem).toIntOrNull() ?: 1
        } catch (e: IndexOutOfBoundsException) {
            Timber.e(e)
        }
    }

    fun setAdapter(arrayAdapter: ArrayAdapter) {
        this.arrayAdapter = arrayAdapter.apply {
            onItemClickListener?.let { setOnItemClickListener(it) }
        }

        popupWindowBinding.recyclerView.apply {
            adapter = arrayAdapter
            layoutManager = LinearLayoutManagerWrapper(context)
        }
    }

    fun setText(text: String) {
        viewBinding.rollingTextView.currentItem = text.toIntOrNull() ?: 1
    }

    class ArrayAdapter(private val array: Array<String>) : RecyclerView.Adapter<ArrayAdapter.ViewHolder>() {
        private var onItemClickListener: OnItemClickListener? = null

        fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
            this.onItemClickListener = onItemClickListener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)

            return ViewHolder(DropdownItemBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position, array[position])
        }

        override fun getItemCount(): Int {
            return array.count()
        }

        fun getItem(position: Int) = array[position]

        inner class ViewHolder(private val viewBinding: DropdownItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
            fun bind(position: Int, item: String) {
                with(viewBinding.root) {
                    text = item

                    setOnClickListener {
                        onItemClickListener?.onItemClick(position, item)
                    }
                }
            }
        }
    }
}
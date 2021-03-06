package com.duke.orca.android.kotlin.biblelockscreen.application.color.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.*
import com.duke.orca.android.kotlin.biblelockscreen.application.color.ColorCalculator
import com.duke.orca.android.kotlin.biblelockscreen.databinding.ColorItemBinding

class ColorAdapter(
    private val colors: IntArray,
    @ColorInt private var pickedColor: Int,
    private val onColorPicked: (color: Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
    private var pickedColorPosition = -1
    private var recyclerView: RecyclerView? = null

    init {
        val index = colors.indexOf(pickedColor)

        if (index.not(-1)) {
            pickedColorPosition = index
        }
    }

    inner class ViewHolder(private val viewBinding: ColorItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(position: Int) {
            val color = colors[position]

            with(viewBinding) {
                val context = root.context

                val dark = ContextCompat.getColor(context, R.color.surface_dark)
                val light = ContextCompat.getColor(context, R.color.surface_light)

                imageView.setColorFilter(
                    ColorCalculator.onBackground(color, dark, light, true),
                    PorterDuff.Mode.SRC_ATOP
                )

                if (pickedColorPosition.`is`(position)) {
                    imageView.scale(
                        0.5F,
                        1.0F,
                        0.0F,
                        1.0F)
                } else if (imageView.isVisible) {
                    imageView.scale(
                        1.0F,
                        0.5F,
                        1.0F,
                        0.0F) {
                        imageView.hide()
                    }
                }

                materialCardView.setCardBackgroundColor(color)

                materialCardView.setOnClickListener {
                    val previousPickedColorPosition = pickedColorPosition

                    if (pickedColorPosition.`is`(position)) {
                        pickedColorPosition = -1

                        notifyItemChanged(previousPickedColorPosition)

                        onColorPicked(0)
                    } else {
                        pickedColorPosition = position

                        notifyItemChanged(previousPickedColorPosition)
                        notifyItemChanged(position)

                        onColorPicked(color)
                    }
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
        this.recyclerView?.itemAnimator = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(ColorItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = colors.count()
}
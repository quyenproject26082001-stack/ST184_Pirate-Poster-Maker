package com.piratemaker.postermaker.poster.activity_app.add_character.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.data.model.SelectedModel
import com.piratemaker.postermaker.poster.databinding.ItemBackgroundColorBinding

class BackgroundColorAdapter :
    ListAdapter<SelectedModel, BackgroundColorAdapter.ViewHolder>(DIFF_CALLBACK) {

    var onChooseColorClick: (() -> Unit) = {}
    var onBackgroundColorClick: ((Int, Int) -> Unit) = { _, _ -> }

    inner class ViewHolder(val binding: ItemBackgroundColorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBackgroundColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d("BackgroundColorAdapter", "onBind position=$position, color=${String.format("#%06X", 0xFFFFFF and item.color)}, isSelected=${item.isSelected}, path=${item.path}")

        holder.binding.apply {
            cardBackgroundColor.foreground = ContextCompat.getDrawable(
                root.context,
                if (item.isSelected) {
                    R.drawable.bg_item_background_selected_foreground
                } else {
                    R.drawable.bg_item_background_unselected_foreground
                }
            )

            if (position == 0) {
                Log.d("BackgroundColorAdapter", "Position 0: Clearing and loading img.png")
                // First clear Glide to stop any pending loads
                Glide.with(root.context).clear(imvColor)
                // Clear any existing drawable
                imvColor.setImageDrawable(null)
                // Clear background completely
                imvColor.background = null
                // Set background to transparent
                imvColor.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // Also clear the parent MaterialCardView's background color
                val cardView = imvColor.parent as? com.google.android.material.card.MaterialCardView
                cardView?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)

                Log.d("BackgroundColorAdapter", "Position 0: Background set to TRANSPARENT (ImageView and CardView), about to load image")
                // Now load the image
                val radiusPx = (4 * root.context.resources.displayMetrics.density).toInt()
                Glide.with(root.context)
                    .load(R.drawable.img_color_bg)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(radiusPx)))
                    .into(imvColor)
                root.tap { onChooseColorClick.invoke() }
            } else {
                Log.d("BackgroundColorAdapter", "Position $position: Setting color background")
                // Clear Glide image and set background color on ImageView
                Glide.with(root.context).clear(imvColor)
                imvColor.setImageDrawable(null)
                imvColor.setBackgroundColor(item.color)

                // Ensure CardView background is also transparent for color positions
                val cardView = imvColor.parent as? com.google.android.material.card.MaterialCardView
                cardView?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)

                root.tap { onBackgroundColorClick.invoke(item.color, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        val selectedPositions = list.mapIndexedNotNull { i, m -> if (m.isSelected) i else null }
        Log.d("BackgroundColorAdapter", "submitItem pos=$position | selectedInList=$selectedPositions | listSize=${list.size}")
        if (position == 0) {
            Log.d("BackgroundColorAdapter", "WARNING: Position 0 was selected!")
        }
        submitList(list.map { it.copy() })
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SelectedModel>() {
            override fun areItemsTheSame(oldItem: SelectedModel, newItem: SelectedModel): Boolean {
                return oldItem.path == newItem.path && oldItem.color == newItem.color
            }

            override fun areContentsTheSame(oldItem: SelectedModel, newItem: SelectedModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}

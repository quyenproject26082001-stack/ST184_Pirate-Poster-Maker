package com.piratemaker.postermaker.poster.activity_app.customize

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.piratemaker.postermaker.poster.core.base.BaseAdapter
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.data.model.custom.ItemColorModel
import com.piratemaker.postermaker.poster.databinding.ItemColorBinding

class ColorLayerCustomizeAdapter(val context: Context) :
    BaseAdapter<ItemColorModel, ItemColorBinding>(ItemColorBinding::inflate) {
    var onItemClick: ((Int) -> Unit) = {}
    override fun onBind(binding: ItemColorBinding, item: ItemColorModel, position: Int) {
        binding.apply {
            imvImage.setBackgroundColor(item.color.toColorInt())
            if (item.isSelected) {
                val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
//                params.setMargins(5.toDp(context), 5.toDp(context), 5.toDp(context), 5.toDp(context))
                cardView.layoutParams = params
            } else {
                val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(0, 0, 0, 0)
                cardView.layoutParams = params
            }
            imvFocus.isVisible = item.isSelected

            root.tap {
                val rv = root.parent as? RecyclerView ?: return@tap
                val currentPosition = rv.getChildAdapterPosition(root)
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClick.invoke(currentPosition)
                }
            }
        }
    }

    fun Int.toDp(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
}
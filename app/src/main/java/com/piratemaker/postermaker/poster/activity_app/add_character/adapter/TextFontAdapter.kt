package com.piratemaker.postermaker.poster.activity_app.add_character.adapter

import android.annotation.SuppressLint
import android.content.Context
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseAdapter
import com.piratemaker.postermaker.poster.core.extensions.setFont
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.data.model.SelectedModel
import com.piratemaker.postermaker.poster.databinding.ItemFontBinding

class TextFontAdapter(val context: Context) : BaseAdapter<SelectedModel, ItemFontBinding>(ItemFontBinding::inflate) {
    var onTextFontClick: ((Int, Int) -> Unit) = { _, _ -> }
    private var currentSelected = 0

    override fun onBind(binding: ItemFontBinding, item: SelectedModel, position: Int) {
        binding.apply {
            tvFont.setFont(item.color)

            if (item.isSelected) {
                // Selected state - set selected background and change text color
                cvMain.setBackgroundResource(R.drawable.bg_text_tool_selected)
                tvFont.setTextColor(android.graphics.Color.parseColor("#7F7F7F"))
            } else {
                // Not selected state - white circle background
                cvMain.setBackgroundResource(R.drawable.bg_text_tool_unselected)
                tvFont.setTextColor(android.graphics.Color.parseColor("#7F7F7F"))
            }

            root.tap { onTextFontClick.invoke(item.color, position) }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        if (position != currentSelected) {
            items.clear()
            items.addAll(list)

            notifyItemChanged(currentSelected)
            notifyItemChanged(position)

            currentSelected = position
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitListReset(list: ArrayList<SelectedModel>){
        items.clear()
        items.addAll(list)
        currentSelected = 0
        notifyDataSetChanged()
    }
}

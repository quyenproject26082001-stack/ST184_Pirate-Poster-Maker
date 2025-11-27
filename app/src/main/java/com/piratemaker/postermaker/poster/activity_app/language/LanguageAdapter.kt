package com.piratemaker.postermaker.poster.activity_app.language

import android.annotation.SuppressLint
import android.content.Context
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseAdapter
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.loadImageGlide
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.data.model.LanguageModel
import com.piratemaker.postermaker.poster.databinding.ItemLanguageBinding

class LanguageAdapter(val context: Context) : BaseAdapter<LanguageModel, ItemLanguageBinding>(
    ItemLanguageBinding::inflate
) {
    var onItemClick: ((String) -> Unit) = {}
    override fun onBind(
        binding: ItemLanguageBinding, item: LanguageModel, position: Int
    ) {
        binding.apply {
            loadImageGlide(root, item.flag, imvFlag, false)
            tvLang.text = item.name

            if (item.activate) {
                loadImageGlide(root, R.drawable.ic_tick_lang, btnRadio, false)
                imvFocus.visible()
            } else {
                loadImageGlide(root, R.drawable.ic_not_tick_lang, btnRadio, false)
                imvFocus.gone()
            }

            root.setOnSingleClick {
                onItemClick.invoke(item.code)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItem(position: Int) {
        items.forEach { it.activate = false }
        items[position].activate = true
        notifyDataSetChanged()
    }
}
package com.charactor.avatar.maker.pfp.activity_app.language

import android.annotation.SuppressLint
import android.content.Context
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseAdapter
import com.charactor.avatar.maker.pfp.core.extensions.gone
import com.charactor.avatar.maker.pfp.core.extensions.loadImageGlide
import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import com.charactor.avatar.maker.pfp.core.extensions.visible
import com.charactor.avatar.maker.pfp.data.model.LanguageModel
import com.charactor.avatar.maker.pfp.databinding.ItemLanguageBinding

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
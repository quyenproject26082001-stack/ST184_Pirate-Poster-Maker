package com.piratemaker.postermaker.poster.activity_app.intro

import android.content.Context
import com.piratemaker.postermaker.poster.core.base.BaseAdapter
import com.piratemaker.postermaker.poster.core.extensions.loadImageGlide
import com.piratemaker.postermaker.poster.core.extensions.select
import com.piratemaker.postermaker.poster.core.extensions.setTextContent
import com.piratemaker.postermaker.poster.core.extensions.strings
import com.piratemaker.postermaker.poster.data.model.IntroModel
import com.piratemaker.postermaker.poster.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImageGlide(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}
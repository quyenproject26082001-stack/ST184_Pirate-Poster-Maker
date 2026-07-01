package com.piratemaker.postermaker.poster.activity_app.add_character.adapter

import com.piratemaker.postermaker.poster.core.base.BaseAdapter
import com.piratemaker.postermaker.poster.core.extensions.loadImage
import com.piratemaker.postermaker.poster.core.extensions.loadImageSticker
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.data.model.SelectedModel
import com.piratemaker.postermaker.poster.databinding.ItemMakerStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemMakerStickerBinding>(ItemMakerStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemMakerStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}
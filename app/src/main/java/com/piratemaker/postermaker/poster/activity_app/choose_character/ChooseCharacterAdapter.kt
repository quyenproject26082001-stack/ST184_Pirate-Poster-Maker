package com.piratemaker.postermaker.poster.activity_app.choose_character

import com.piratemaker.postermaker.poster.core.base.BaseAdapter
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.loadImage
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.data.model.custom.CustomizeModel
import com.piratemaker.postermaker.poster.databinding.ItemChooseAvatarBinding

class ChooseCharacterAdapter : BaseAdapter<CustomizeModel, ItemChooseAvatarBinding>(ItemChooseAvatarBinding::inflate) {
    var onItemClick: ((position: Int) -> Unit) = {}
    override fun onBind(binding: ItemChooseAvatarBinding, item: CustomizeModel, position: Int) {
        binding.apply {
            loadImage(root, item.avatar, imvImage)
            sflShimmer.stopShimmer()
            sflShimmer.gone()
            root.tap { onItemClick.invoke(position) }
        }
    }
}

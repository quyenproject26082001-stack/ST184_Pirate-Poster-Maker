package com.charactor.avatar.maker.pfp.data.model.custom

import com.charactor.avatar.maker.pfp.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf()
)
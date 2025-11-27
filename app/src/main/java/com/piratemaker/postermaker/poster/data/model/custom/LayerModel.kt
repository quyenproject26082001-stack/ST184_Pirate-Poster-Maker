package com.piratemaker.postermaker.poster.data.model.custom

import com.piratemaker.postermaker.poster.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf()
)
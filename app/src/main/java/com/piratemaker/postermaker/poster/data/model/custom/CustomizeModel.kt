package com.piratemaker.postermaker.poster.data.model.custom

data class CustomizeModel(
    val dataName: String = "",
    val avatar: String = "",
    val layerList: ArrayList<LayerListModel> = arrayListOf(),
    val level: Int = 100,
    val isFromAPI: Boolean = false
)

package com.charactor.avatar.maker.pfp.data.model.custom

data class CustomizeModel(
    val dataName: String = "",
    val avatar: String = "",
    val layerList: ArrayList<LayerListModel> = arrayListOf()
)

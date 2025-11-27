package com.piratemaker.postermaker.poster.core.custom.layout

import android.widget.ImageView
import com.piratemaker.postermaker.poster.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}
package com.charactor.avatar.maker.pfp.core.custom.layout

import android.widget.ImageView
import com.charactor.avatar.maker.pfp.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}
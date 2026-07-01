package com.piratemaker.postermaker.poster.core.extensions

import android.content.Context
import android.annotation.SuppressLint
import java.io.File
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.piratemaker.postermaker.poster.core.utils.DataLocal
import com.facebook.shimmer.ShimmerDrawable


fun loadImageGlide(context: Context, path: String, imageView: ImageView, isLoadShimmer: Boolean = true) {
    val shimmerDrawable = ShimmerDrawable().apply {
        setShimmer(DataLocal.shimmer)
    }
    if (isLoadShimmer){
        Glide.with(context).load(path).placeholder(shimmerDrawable).error(shimmerDrawable).into(imageView)
    }else{
        Glide.with(context).load(path).placeholder(shimmerDrawable).error(shimmerDrawable).into(imageView)
    }

}

fun loadImageGlide(viewGroup: ViewGroup, path: String, imageView: ImageView, isLoadShimmer: Boolean = true) {
    val shimmerDrawable = ShimmerDrawable().apply {
        setShimmer(DataLocal.shimmer)
    }
    if (isLoadShimmer){
        Glide.with(viewGroup).load(path).placeholder(shimmerDrawable).error(shimmerDrawable).into(imageView)
    }else{
        Glide.with(viewGroup).load(path).placeholder(shimmerDrawable).error(shimmerDrawable).into(imageView)
    }
}

fun loadImageGlide(viewGroup: ViewGroup, path: Int, imageView: ImageView, isLoadShimmer: Boolean = true) {
    val shimmerDrawable = ShimmerDrawable().apply {
        setShimmer(DataLocal.shimmer)
    }
    if (isLoadShimmer){
        Glide.with(viewGroup).load(path).placeholder(shimmerDrawable).error(shimmerDrawable).into(imageView)
    }else{
        Glide.with(viewGroup).load(path).into(imageView)
    }
}

fun loadImage(context: Context, path: String, imageView: ImageView, isLoadShimmer: Boolean = true) {
    loadImageGlide(context, path, imageView, isLoadShimmer)
}

fun loadImage(viewGroup: ViewGroup, path: String, imageView: ImageView, isLoadShimmer: Boolean = true) {
    loadImageGlide(viewGroup, path, imageView, isLoadShimmer)
}

fun loadImage(viewGroup: ViewGroup, path: Int, imageView: ImageView, isLoadShimmer: Boolean = true) {
    loadImageGlide(viewGroup, path, imageView, isLoadShimmer)
}

fun loadImageSticker(
    viewGroup: ViewGroup,
    path: String,
    imageView: ImageView,
    isLoadShimmer: Boolean = true
) {
    val shimmerDrawable = ShimmerDrawable().apply {
        setShimmer(DataLocal.shimmer)
    }
    val request = Glide.with(viewGroup)
        .load(path)
        .override(256, 256)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .transform(RoundedCorners(24))
    if (isLoadShimmer) {
        request.placeholder(shimmerDrawable).error(shimmerDrawable).into(imageView)
    } else {
        request.into(imageView)
    }
}

@SuppressLint("CheckResult")
fun ImageView.loadImageFromFile(path: String) {
    Glide.with(context)
        .load(File(path))
        .skipMemoryCache(true)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .into(this)
}

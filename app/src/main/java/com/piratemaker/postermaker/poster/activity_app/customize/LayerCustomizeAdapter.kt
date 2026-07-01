package com.piratemaker.postermaker.poster.activity_app.customize

import android.content.Context
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.utils.DataLocal
import com.piratemaker.postermaker.poster.core.utils.key.AssetsKey
import com.piratemaker.postermaker.poster.data.model.custom.ItemNavCustomModel
import com.piratemaker.postermaker.poster.databinding.ItemCustomizeBinding
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerDrawable

class LayerCustomizeAdapter(val context: Context) : ListAdapter<ItemNavCustomModel, LayerCustomizeAdapter.CustomizeViewHolder>(DiffCallback) {

    var onItemClick: ((ItemNavCustomModel, Int) -> Unit) = { _, _ -> }
    var onNoneClick: ((Int) -> Unit) = {}
    var onRandomClick: (() -> Unit) = {}

    inner class CustomizeViewHolder(val binding: ItemCustomizeBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(item: ItemNavCustomModel, position: Int) {
            binding.apply {
                val shimmerDrawable = ShimmerDrawable().apply {
                    setShimmer(DataLocal.shimmer)
                    startShimmer()
                }

                // Apply rounded corners using ViewOutlineProvider (40dp radius like ShapeAppearanceOverlay.Pony.Rounded20)
                val cornerRadius = 8f * context.resources.displayMetrics.density
                imvImage.clipToOutline = true
                imvImage.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                    }
                }

                if (item.isSelected) {
                    // Bring selected item to front with elevation
                    root.translationZ = 16f
                    root.scaleX = 1.0f
                    root.scaleY = 1.0f
                    vFocus.gone()

                    cardLayerItem.setBackgroundResource(R.drawable.bg_item_customize_selected)
                    // vFocus.setBackgroundResource(R.drawable.bg_10_stroke_yellow)
                } else {
                    // Reset to normal state
                    root.translationZ = 0f
                    root.scaleX = 1f
                    root.scaleY = 1f
                    vFocus.gone()
                    cardLayerItem.setBackgroundResource(R.drawable.bg_item_customize_unselected)


                }

                when (item.path) {
                    AssetsKey.NONE_LAYER -> {
                        btnNone.visible()
                        btnRandom.gone()
                        imvImage.gone()
                    }
                    AssetsKey.RANDOM_LAYER -> {
                        btnNone.gone()
                        btnRandom.visible()
                        imvImage.gone()
                    }
                    else -> {
                        btnNone.gone()
                        imvImage.visible()
                        btnRandom.gone()
                        Glide.with(root).load(item.thumb.ifEmpty { item.path }).placeholder(shimmerDrawable).into(imvImage)
                    }
                }

                binding.imvImage.tap(100) { onItemClick.invoke(item, position) }

                binding.btnRandom.tap { onRandomClick.invoke() }

                binding.btnNone.tap { onNoneClick.invoke(position) }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomizeViewHolder {
        return CustomizeViewHolder(ItemCustomizeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CustomizeViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<ItemNavCustomModel>(){
            override fun areItemsTheSame(oldItem: ItemNavCustomModel, newItem: ItemNavCustomModel): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: ItemNavCustomModel, newItem: ItemNavCustomModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}

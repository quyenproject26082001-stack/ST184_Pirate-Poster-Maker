package com.charactor.avatar.maker.pfp.activity_app.posterwanted

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.charactor.avatar.maker.pfp.databinding.ItemPosterWantedTemplateBinding

class PosterWantedTemplateAdapter(
    private val items: List<PosterWantedItem>,
    private val onItemClick: (PosterWantedItem) -> Unit
) : RecyclerView.Adapter<PosterWantedTemplateAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPosterWantedTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemPosterWantedTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PosterWantedItem) {
            // Load template background
            Glide.with(binding.root.context)
                .load(item.getTemplatePath())
                .into(binding.imgTemplate)

            // Load avatar
            Glide.with(binding.root.context)
                .load(item.getAvatarPath())
                .centerCrop()
                .into(binding.imgAvatar)

            // Set texts
            binding.tvName.text = item.name
            binding.tvBounty.text = item.bounty

            // Click listener
            binding.cardItem.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

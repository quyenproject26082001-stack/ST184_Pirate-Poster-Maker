package com.piratemaker.postermaker.poster.activity_app.mycreation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.piratemaker.postermaker.poster.databinding.ItemMyCharacterBinding
import com.piratemaker.postermaker.poster.databinding.ItemMyDesignBinding
import java.io.File

class MyCreationAdapter(
    private var items: List<File>,
    private var isMyWantedTab: Boolean = false,
    private var isCharacterTab: Boolean = false,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class DesignViewHolder(private val binding: ItemMyDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File, isMyWanted: Boolean) {
            Glide.with(binding.root.context)
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))
                .into(binding.imgDesign)

            // Set margins based on tab type
            val layoutParams = binding.rootContainer.layoutParams as ViewGroup.MarginLayoutParams
            if (isMyWanted) {
                // MY_WANTED tab: set margins to 0
                layoutParams.setMargins(0, 0, 0, 0)
            } else {
                // MY_DESIGN tab: use default margins
                val horizontalMargin = binding.root.context.resources.displayMetrics.density * 4
                val bottomMargin = binding.root.context.resources.displayMetrics.density * 8
                layoutParams.setMargins(
                    horizontalMargin.toInt(),
                    0,
                    horizontalMargin.toInt(),
                    bottomMargin.toInt()
                )
            }
            binding.rootContainer.layoutParams = layoutParams

            binding.root.setOnClickListener {
                onItemClick(file)
            }
        }
    }

    inner class CharacterViewHolder(private val binding: ItemMyCharacterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            Glide.with(binding.root.context)
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))
                .into(binding.imgDesign)

            binding.root.setOnClickListener {
                onItemClick(file)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isCharacterTab) VIEW_TYPE_CHARACTER else VIEW_TYPE_DESIGN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_CHARACTER) {
            val binding = ItemMyCharacterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            CharacterViewHolder(binding)
        } else {
            val binding = ItemMyDesignBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            DesignViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = items[position]
        when (holder) {
            is CharacterViewHolder -> holder.bind(file)
            is DesignViewHolder -> holder.bind(file, isMyWantedTab)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<File>, isMyWanted: Boolean = false, isCharacter: Boolean = false) {
        items = newItems
        isMyWantedTab = isMyWanted
        isCharacterTab = isCharacter
        notifyDataSetChanged()
    }

    private companion object {
        const val VIEW_TYPE_DESIGN = 0
        const val VIEW_TYPE_CHARACTER = 1
    }
}

package com.piratemaker.postermaker.poster.activity_app.oldwest

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.piratemaker.postermaker.poster.databinding.ItemMyDesignBinding

class OldWestAdapter(
    private val context: Context,
    private var items: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<OldWestAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMyDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(assetFileName: String) {
            try {
                // Load image from assets folder
                val inputStream = context.assets.open("oldwest/$assetFileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.imgDesign.setImageBitmap(bitmap)
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            binding.root.setOnClickListener {
                onItemClick(assetFileName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyDesignBinding.inflate(
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

    fun updateItems(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}

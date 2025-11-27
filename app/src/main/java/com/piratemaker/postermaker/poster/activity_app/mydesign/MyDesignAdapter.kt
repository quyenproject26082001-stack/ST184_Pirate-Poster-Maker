package com.piratemaker.postermaker.poster.activity_app.mydesign

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.piratemaker.postermaker.poster.databinding.ItemMyDesignBinding
import java.io.File

class MyDesignAdapter(
    private var items: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<MyDesignAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMyDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            Glide.with(binding.root.context)
                .load(file)
                .into(binding.imgDesign)

            binding.root.setOnClickListener {
                onItemClick(file)
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

    fun updateItems(newItems: List<File>) {
        items = newItems
        notifyDataSetChanged()
    }
}

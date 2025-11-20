package com.charactor.avatar.maker.pfp.activity_app.posterwanted

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.databinding.ItemPosterWantedTemplateBinding
import kotlinx.coroutines.*

class PosterWantedTemplateAdapter(
    private val items: List<PosterWantedItem>,
    private val onItemClick: (PosterWantedItem) -> Unit
) : RecyclerView.Adapter<PosterWantedTemplateAdapter.ViewHolder>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelJob()
    }

    fun cleanup() {
        coroutineScope.cancel()
    }

    inner class ViewHolder(
        private val binding: ItemPosterWantedTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentJob: Job? = null

        fun bind(item: PosterWantedItem) {
            val context = binding.root.context

            // Cancel previous job if any
            currentJob?.cancel()

            // Clear previous image
            binding.imgRenderedPoster.setImageBitmap(null)

            // Click listener
            binding.rootContainer.setOnClickListener {
                onItemClick(item)
            }

            // Render bitmap in background
            currentJob = coroutineScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        renderTemplateToBitmap(context, item)
                    }
                    if (isActive) {
                        binding.imgRenderedPoster.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun cancelJob() {
            currentJob?.cancel()
        }

        private suspend fun renderTemplateToBitmap(
            context: android.content.Context,
            item: PosterWantedItem
        ): Bitmap {
            return withContext(Dispatchers.Main) {
                // Get the correct template layout resource
                val layoutResId = getTemplateLayoutResId(item.templateId)

                // Inflate the template layout
                val templateView = LayoutInflater.from(context).inflate(layoutResId, null)

                // Find views in the inflated template
                val imgTemplate = templateView.findViewById<ImageView>(R.id.imgTemplate)
                val imgAvatar = templateView.findViewById<ImageView>(R.id.imgAvatar)
                val tvName = templateView.findViewById<TextView>(R.id.tvName)
                val tvBounty = templateView.findViewById<TextView>(R.id.tvBounty)

                // Set texts
                tvName.text = item.name
                tvBounty.text = item.bounty

                // Load images synchronously
                val templateBitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(item.getTemplatePath())
                        .submit()
                        .get()
                }

                val avatarBitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(item.getAvatarPath())
                        .submit()
                        .get()
                }

                // Set loaded bitmaps to ImageViews
                imgTemplate.setImageBitmap(templateBitmap)
                imgAvatar.setImageBitmap(avatarBitmap)

                // Measure and layout the view
                // Use 3:4 aspect ratio, width = 600px (reasonable for thumbnail)
                val width = 600
                val height = 800

                val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

                templateView.measure(widthSpec, heightSpec)
                templateView.layout(0, 0, width, height)

                // Create bitmap and draw the view
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                templateView.draw(canvas)

                bitmap
            }
        }

        private fun getTemplateLayoutResId(templateId: Int): Int {
            return when (templateId) {
                1 -> R.layout.layout_poster_template_1
                2 -> R.layout.layout_poster_template_2
                3 -> R.layout.layout_poster_template_3
                4 -> R.layout.layout_poster_template_4
                5 -> R.layout.layout_poster_template_5
                6 -> R.layout.layout_poster_template_6
                7 -> R.layout.layout_poster_template_7
                8 -> R.layout.layout_poster_template_8
                9 -> R.layout.layout_poster_template_9
                10 -> R.layout.layout_poster_template_10
                11 -> R.layout.layout_poster_template_11
                12 -> R.layout.layout_poster_template_12
                13 -> R.layout.layout_poster_template_13
                14 -> R.layout.layout_poster_template_14
                15 -> R.layout.layout_poster_template_15
                16 -> R.layout.layout_poster_template_16
                else -> R.layout.layout_poster_template_1
            }
        }
    }
}

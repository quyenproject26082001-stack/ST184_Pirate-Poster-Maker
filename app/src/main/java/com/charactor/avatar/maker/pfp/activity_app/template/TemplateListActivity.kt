package com.charactor.avatar.maker.pfp.activity_app.template

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.gone
import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import com.charactor.avatar.maker.pfp.core.extensions.visible
import com.charactor.avatar.maker.pfp.databinding.ActivityTemplateListBinding

class TemplateListActivity : BaseActivity<ActivityTemplateListBinding>() {

    private lateinit var adapter: TemplateAdapter
    private var selectedTemplateId = 1

    override fun setViewBinding(): ActivityTemplateListBinding {
        return ActivityTemplateListBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Get current template ID from intent
        selectedTemplateId = intent.getIntExtra("currentTemplateId", 1)

        setupRecyclerView()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Back (Home icon)
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            // Center text - Title

            // Right button - Done (Check icon)
            btnActionBarRight.setImageResource(R.drawable.ic_done)
            btnActionBarRight.visible()

            // Hide others
            btnActionBarRightText.gone()
            tvRightText.gone()
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            // Back button
            btnActionBarLeft.setOnSingleClick {
                finish()
            }

            // Done button
            btnActionBarRight.setOnSingleClick {
                // Return selected template ID to caller
                val resultIntent = Intent().apply {
                    putExtra("selectedTemplateId", selectedTemplateId)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        // Create 16 templates (matching assets/template/1 to assets/template/16)
        val templates = (1..16).map { id ->
            TemplateItem(id, "Template $id")
        }

        adapter = TemplateAdapter(templates, selectedTemplateId) { templateId ->
            selectedTemplateId = templateId
        }

        val layoutManager = CenterZoomLayoutManager(this)
        binding.rvTemplates.layoutManager = layoutManager
        binding.rvTemplates.adapter = adapter

        // Snap helper to center items
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvTemplates)

        // Scroll to current selected template
        val initialPosition = selectedTemplateId - 1  // templateId starts at 1
        binding.rvTemplates.scrollToPosition(initialPosition)

        // Add scroll listener for zoom effect and auto-selection
        binding.rvTemplates.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                scaleMiddleItem(recyclerView)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scaleMiddleItem(recyclerView)
                    // Auto-select centered item when scroll stops
                    updateSelectedTemplateFromCenter(recyclerView, snapHelper)
                }
            }
        })

        // Initial scale
        binding.rvTemplates.post {
            scaleMiddleItem(binding.rvTemplates)
        }
    }

    /**
     * Update selectedTemplateId based on the centered/snapped item
     */
    private fun updateSelectedTemplateFromCenter(recyclerView: RecyclerView, snapHelper: LinearSnapHelper) {
        val layoutManager = recyclerView.layoutManager ?: return
        val snappedView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snappedView)

        // Template ID is position + 1 (since positions are 0-indexed)
        val newTemplateId = position + 1

        if (selectedTemplateId != newTemplateId) {
            selectedTemplateId = newTemplateId
            // Update adapter visual selection using efficient method
            adapter.setSelectedPosition(position)
        }
    }

    private fun scaleMiddleItem(recyclerView: RecyclerView) {
        val midpoint = recyclerView.width / 2f
        val minScale = 0.7f
        val maxScale = 1.0f

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childMidpoint = (child.left + child.right) / 2f
            val distance = Math.abs(midpoint - childMidpoint)

            // Calculate scale based on distance from center
            val scale = maxScale - minScale * (distance / recyclerView.width)
            val finalScale = Math.max(minScale, Math.min(maxScale, scale))

            child.scaleX = finalScale
            child.scaleY = finalScale

            // Optional: Add elevation/translation effect
            child.translationZ = finalScale * 10f
        }
    }
}

// Layout Manager for center zoom effect
class CenterZoomLayoutManager(context: Context) :
    LinearLayoutManager(context, HORIZONTAL, false) {

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        scaleChildren()
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
        scaleChildren()
        return scrolled
    }

    private fun scaleChildren() {
        val midpoint = width / 2f
        val minScale = 0.7f
        val maxScale = 1.0f

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childMidpoint = (getDecoratedLeft(child) + getDecoratedRight(child)) / 2f
            val distance = Math.abs(midpoint - childMidpoint)

            val scale = maxScale - minScale * (distance / width)
            val finalScale = Math.max(minScale, Math.min(maxScale, scale))

            child.scaleX = finalScale
            child.scaleY = finalScale
        }
    }
}


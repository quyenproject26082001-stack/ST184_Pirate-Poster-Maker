package com.charactor.avatar.maker.pfp.activity_app.mydesign

import android.content.Intent
import android.view.LayoutInflater
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.databinding.ActivityMyDesignBinding
import java.io.File

class MyDesignActivity : BaseActivity<ActivityMyDesignBinding>() {

    private lateinit var adapter: MyDesignAdapter

    override fun setViewBinding(): ActivityMyDesignBinding {
        return ActivityMyDesignBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        loadDesigns()
    }

    override fun onResume() {
        super.onResume()
        // Reload designs when returning from ViewDesignActivity (in case of deletion)
        if (::adapter.isInitialized) {
            loadDesigns()
        }
    }

    private fun loadDesigns() {
        val postersDir = File(filesDir, "posters")
        val designs = if (postersDir.exists()) {
            postersDir.listFiles()
                ?.filter { it.isFile && (it.extension == "png" || it.extension == "jpg" || it.extension == "jpeg") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }

        if (designs.isEmpty()) {
            binding.rvDesigns.gone()
            binding.tvEmpty.visible()
        } else {
            binding.rvDesigns.visible()
            binding.tvEmpty.gone()

            if (::adapter.isInitialized) {
                adapter.updateItems(designs)
            } else {
                adapter = MyDesignAdapter(designs) { file ->
                    onDesignClicked(file)
                }
                binding.rvDesigns.adapter = adapter
            }
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            btnActionBarLeft.setOnSingleClick {
                finishAfterTransition()
            }
        }
    }

    override fun dataObservable() {
        // No data observables needed
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            tvCenter.text = strings(R.string.my_design)
            tvCenter.gone()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
        }
    }

    private fun onDesignClicked(file: File) {
        val intent = Intent(this, ViewDesignActivity::class.java).apply {
            putExtra("imagePath", file.absolutePath)
        }
        startActivity(intent)
    }
}

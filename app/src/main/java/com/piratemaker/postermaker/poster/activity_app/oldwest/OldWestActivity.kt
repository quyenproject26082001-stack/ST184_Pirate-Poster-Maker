package com.piratemaker.postermaker.poster.activity_app.oldwest

import android.content.Intent
import android.view.LayoutInflater
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.*

//quyen
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.databinding.ActivityOldWestBinding

//quyen

class OldWestActivity : BaseActivity<ActivityOldWestBinding>() {

    private lateinit var adapter: OldWestAdapter

    override fun setViewBinding(): ActivityOldWestBinding {
        return ActivityOldWestBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        loadDesigns()
    }

    override fun onResume() {
        super.onResume()
        // Reload designs when returning from ViewOldWestActivity
        if (::adapter.isInitialized) {
            loadDesigns()
        }
    }

    private fun loadDesigns() {
        try {
            // Load images from assets/oldwest folder
            val assetFiles = assets.list("oldwest")
                ?.filter { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") }
                ?.sorted()
                ?: emptyList()

            if (assetFiles.isEmpty()) {
                binding.rvDesigns.gone()
                binding.tvEmpty.visible()
            } else {
                binding.rvDesigns.visible()
                binding.tvEmpty.gone()

                if (::adapter.isInitialized) {
                    adapter.updateItems(assetFiles)
                } else {
                    adapter = OldWestAdapter(this, assetFiles) { fileName ->
                        onDesignClicked(fileName)
                    }
                    binding.rvDesigns.adapter = adapter
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.rvDesigns.gone()
            binding.tvEmpty.visible()
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
            tvCenter.text = "Old West"
            tvCenter.gone()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
        }
    }

    //quyen
    override fun initAds() {
        // Load native regular ad above back button and list
        Admob.getInstance().loadNativeAd(this, getString(R.string.native_myDesgin), binding.nativeMyDesgin, R.layout.ads_native_collap_banner_1)

        // Load native collapsible ad at bottom
        Admob.getInstance().loadNativeCollap(this, getString(R.string.native_collap_myDesgin), binding.nativeCollapMyDesgin)
    }
    //quyen

    private fun onDesignClicked(fileName: String) {
        val intent = Intent(this, ViewOldWestActivity::class.java).apply {
            putExtra("assetFileName", fileName)
        }
        startActivity(intent)
    }
}

package com.piratemaker.postermaker.poster.activity_app.mycreation

import android.content.Intent
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.maker_view.ViewActivity
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.*
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.core.utils.key.IntentKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.data.model.custom.SuggestionModel
import com.piratemaker.postermaker.poster.databinding.ActivityMyCreationBinding
import java.io.File
//quyen
import com.lvt.ads.util.Admob
//quyen

class MyCreationActivity : BaseActivity<ActivityMyCreationBinding>() {

    private lateinit var adapter: MyCreationAdapter
    private var currentTab = TabType.MY_DESIGN // Default to My Design

    // ActivityResult launcher for ViewCreationActivity
    private val viewCreationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Reload designs when returning (in case image was edited)
        loadDesigns()
    }

    enum class TabType {
        MY_DESIGN,  // Bounty photos
        MY_WANTED,  // Wanted posters
        CHARACTER
    }

    override fun setViewBinding(): ActivityMyCreationBinding {
        return ActivityMyCreationBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        currentTab = getInitialTab()
        setupTabs()
        loadDesigns()
    }

    override fun onResume() {
        super.onResume()
        // Reload designs when returning from ViewDesignActivity (in case of deletion)
        if (::adapter.isInitialized) {
            loadDesigns()
        }
    }

    private fun setupTabs() {
        updateTabSelection(currentTab)

        binding.apply {
            tabMyDesign.setOnSingleClick {
                if (currentTab != TabType.MY_DESIGN) {
                    currentTab = TabType.MY_DESIGN
                    updateTabSelection(TabType.MY_DESIGN)
                    loadDesigns()
                }
            }

            tabMyWanted.setOnSingleClick {
                if (currentTab != TabType.MY_WANTED) {
                    currentTab = TabType.MY_WANTED
                    updateTabSelection(TabType.MY_WANTED)
                    loadDesigns()
                }
            }

            tabCharacter.setOnSingleClick {
                if (currentTab != TabType.CHARACTER) {
                    currentTab = TabType.CHARACTER
                    updateTabSelection(TabType.CHARACTER)
                    loadDesigns()
                }
            }
        }
    }

    private fun getInitialTab(): TabType {
        return when (intent.getIntExtra(IntentKey.TAB_KEY, ValueKey.MY_DESIGN_TYPE)) {
            ValueKey.AVATAR_TYPE -> TabType.CHARACTER
            ValueKey.MY_WANTED_TYPE -> TabType.MY_WANTED
            else -> TabType.MY_DESIGN
        }
    }

    private fun updateTabSelection(selectedTab: TabType) {
        binding.apply {
            when (selectedTab) {
                TabType.MY_DESIGN -> {
                    tabMyDesign.isSelected = true
                    tabMyWanted.isSelected = false
                    tabCharacter.isSelected = false
                }
                TabType.MY_WANTED -> {
                    tabMyDesign.isSelected = false
                    tabMyWanted.isSelected = true
                    tabCharacter.isSelected = false
                }
                TabType.CHARACTER -> {
                    tabMyDesign.isSelected = false
                    tabMyWanted.isSelected = false
                    tabCharacter.isSelected = true
                }
            }
            updateTabHeight(tabMyDesign)
            updateTabHeight(tabMyWanted)
            updateTabHeight(tabCharacter)
        }
    }

    private fun updateTabHeight(tab: TextView) {
        val selectedHeight = (40 * resources.displayMetrics.density).toInt()
        val unselectedHeight = (32 * resources.displayMetrics.density).toInt()
        tab.layoutParams = tab.layoutParams.apply {
            height = if (tab.isSelected) selectedHeight else unselectedHeight
        }
    }

    private fun loadDesigns() {
        val designs = when (currentTab) {
            TabType.CHARACTER -> loadCharacterDesigns()
            else -> loadImageFilesForCurrentTab()
        }

        if (designs.isEmpty()) {
            binding.rvDesigns.gone()
            binding.tvEmpty.visible()
        } else {
            binding.rvDesigns.visible()
            binding.tvEmpty.gone()

            val isMyWanted = currentTab == TabType.MY_WANTED
            val isCharacter = currentTab == TabType.CHARACTER

            if (::adapter.isInitialized) {
                adapter.updateItems(designs, isMyWanted, isCharacter)
            } else {
                adapter = MyCreationAdapter(designs, isMyWanted, isCharacter) { file ->
                    onDesignClicked(file)
                }
                binding.rvDesigns.adapter = adapter
            }
        }
    }

    private fun loadImageFilesForCurrentTab(): List<File> {
        val folderName = when (currentTab) {
            TabType.MY_DESIGN -> "bounty_designs"
            TabType.MY_WANTED -> "posters"
            TabType.CHARACTER -> return emptyList()
        }

        val postersDir = File(filesDir, folderName)
        return if (postersDir.exists()) {
            postersDir.listFiles()
                ?.filter { it.isFile && (it.extension == "png" || it.extension == "jpg" || it.extension == "jpeg") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun loadCharacterDesigns(): List<File> {
        return MediaHelper.readListFromFile<SuggestionModel>(this, ValueKey.EDIT_FILE_INTERNAL)
            .mapNotNull { model ->
                File(model.pathInternalEdit).takeIf { it.exists() && it.isFile }
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

    override fun onRestart() {
        super.onRestart()
        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_myDesgin), binding.nativeCollapMyDesgin)

    }
    //quyen
    override fun initAds() {
        // Load native regular ad above back button and list
        Admob.getInstance().loadNativeAd(this, getString(R.string.native_myDesgin), binding.nativeMyDesgin, R.layout.ads_native_collap_banner_1)

        // Load native collapsible ad at bottom
        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_myDesgin), binding.nativeCollapMyDesgin)
    }
    //quyen

    private fun onDesignClicked(file: File) {
        if (currentTab == TabType.CHARACTER) {
            val intent = Intent(this, ViewActivity::class.java).apply {
                putExtra(IntentKey.INTENT_KEY, file.absolutePath)
                putExtra(IntentKey.STATUS_KEY, ValueKey.AVATAR_TYPE)
            }
            showInterAll { viewCreationLauncher.launch(intent) }
            return
        }

        val intent = Intent(this, ViewCreationActivity::class.java).apply {
            putExtra("imagePath", file.absolutePath)

            putExtra("isMyDesign", currentTab == TabType.MY_DESIGN)
        }
        showInterAll { viewCreationLauncher.launch(intent) }
    }
}

package com.piratemaker.postermaker.poster.activity_app.customize

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.dLog
import com.piratemaker.postermaker.poster.core.extensions.eLog
import com.piratemaker.postermaker.poster.core.extensions.hideNavigation
import com.piratemaker.postermaker.poster.core.extensions.invisible
import com.piratemaker.postermaker.poster.core.extensions.loadNativeCollabAds
import com.piratemaker.postermaker.poster.core.extensions.logEvent
import com.piratemaker.postermaker.poster.core.extensions.setImageActionBar
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.showToast
import com.piratemaker.postermaker.poster.core.extensions.startIntentLeftToRight
import com.piratemaker.postermaker.poster.core.extensions.startIntentRightToLeft
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.LanguageHelper
import com.piratemaker.postermaker.poster.core.utils.key.IntentKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.SaveState
import com.piratemaker.postermaker.poster.data.model.custom.ItemNavCustomModel
import com.piratemaker.postermaker.poster.databinding.ActivityCustomizeBinding
import com.piratemaker.postermaker.poster.dialog.DialogType
import com.piratemaker.postermaker.poster.dialog.YesNoDialog
import com.piratemaker.postermaker.poster.activity_app.main.DataViewModel
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.data.model.custom.SuggestionModel
import com.piratemaker.postermaker.poster.activity_app.add_character.AddCharacterActivity
import com.piratemaker.postermaker.poster.activity_app.mycreation.MyCreationActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.get
import kotlin.jvm.java
import com.bumptech.glide.request.target.Target
import com.ironsource.adqualitysdk.sdk.i.iv
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.tapAndHold
import kotlin.math.roundToInt


class CustomizeCharacterActivity : BaseActivity<ActivityCustomizeBinding>() {

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 1.5f
        private const val ADD_CHARACTER_OPEN_DEBOUNCE_MS = 3000L
        private var lastAddCharacterOpenTime = 0L

        @Synchronized
        private fun canOpenAddCharacterNow(): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastAddCharacterOpenTime < ADD_CHARACTER_OPEN_DEBOUNCE_MS) {
                return false
            }
            lastAddCharacterOpenTime = now
            return true
        }
    }

    private val viewModel: CustomizeCharacterViewModel by viewModels()
    private var lastClickedLayerPosition: Int =
        -1 // Track last clicked layer position for scrolling
    private val dataViewModel: DataViewModel by viewModels()
    val colorLayerCustomizeAdapter by lazy { ColorLayerCustomizeAdapter(this) }
    val layerCustomizeAdapter by lazy { LayerCustomizeAdapter(this) }
    val bottomNavigationCustomizeAdapter by lazy { BottomNavigationCustomizeAdapter(this) }
    private var isSavingCharacter = false
    private var isOpeningAddCharacter = false
    val hideList: ArrayList<View> by lazy {
        arrayListOf(
            binding.color,
            binding.rcvLayer,
            binding.layoutMove,
            binding.flBottomNav,
            binding.btnMove,
            binding.btnResetCustomize,
            binding.btnFlip,
            binding.flBottomNav,

            )
    }

    override fun setViewBinding(): ActivityCustomizeBinding {
        return ActivityCustomizeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initRcv()
        lifecycleScope.launch { showLoading() }
        dataViewModel.ensureData(this)


    }

    override fun dataObservable() {
        lifecycleScope.launch {
            launch {
                dataViewModel.allData.collect { list ->
                    if (list.isNotEmpty()) {
                        viewModel.positionSelected = intent.getIntExtra(IntentKey.INTENT_KEY, 0)
                        viewModel.statusFrom =
                            intent.getIntExtra(IntentKey.STATUS_FROM_KEY, ValueKey.CREATE)
                        val safePosition = viewModel.positionSelected.coerceIn(0, list.size - 1)
                        viewModel.setDataCustomize(list[safePosition])
                        viewModel.setIsDataAPI(list[safePosition].isFromAPI)
                        initData()
                    }
                }
            }
            launch {
                viewModel.isFlip.collect { status ->
                    val rotation = if (status) -180f else 0f
                    viewModel.imageViewList.forEachIndexed { index, view ->
                        view.rotationY = rotation
                    }
                }
            }
            launch {
                viewModel.isHideView.collect { status ->
                    if (viewModel.isCreated.value) {
                        val res = if (status) {
                            hideList.forEach { it.invisible() }
                            R.drawable.ic_hide
                        } else {
                            hideList.forEach { it.visible() }
                            checkStatusColor()
                            R.drawable.ic_show

                        }
                        binding.btnHide.setImageResource(res)
                    }
                }
            }
            launch {
                viewModel.bottomNavigationList.collect { bottomNavigationList ->
                    if (bottomNavigationList.isNotEmpty()) {
                        bottomNavigationCustomizeAdapter.submitList(bottomNavigationList)
                        layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                        colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                        scrollSelectedColorIntoView()
                    }
                }
            }
        }
    }


    private fun isTransformChanged(): Boolean {
        val transform =
            viewModel.layerTransformList[viewModel.positionCustom]
        return transform.translationX != 0f ||
                transform.translationY != 0f ||
                transform.rotation != 0f || transform.scaleX !=
                1f || transform.scaleY != 1f
    }


    private fun updateResetBtn() {
        val changed = isTransformChanged()
        binding.btnResetMove.alpha = if (changed) 1f else 0.4f
        binding.btnResetMove.isEnabled = changed
    }


    private fun updateScaleButtons() {
        if (viewModel.positionCustom !in viewModel.imageViewList.indices) return
        val progress = scaleToProgress(viewModel.imageViewList[viewModel.positionCustom].scaleX)
        if (binding.SeekBar.progress != progress) {
            binding.SeekBar.progress = progress
        }
        updateIcThumb(progress)
    }

    private fun updateMoveButtons() {
        val iv = viewModel.imageViewList[viewModel.positionCustom]
        val maxX = binding.layoutCustomLayer.width / 2f
        val maxY = binding.layoutCustomLayer.height / 2f
        dLog("updateMoveButtons: translationX=${iv.translationX}, translationY=${iv.translationY}, maxX=$maxX, maxY=$maxY")

        binding.btnMoveLeft.apply {
            val reached = iv.translationX <= -maxX
            alpha = if (reached) 0.4f else 1f
            isEnabled = !reached
        }
        binding.btnMoveRight.apply {
            val reached = iv.translationX >= maxX
            alpha = if (reached) 0.4f else 1f
            isEnabled = !reached
        }
        binding.btnMoveUp.apply {
            val reached = iv.translationY <= -maxY
            alpha = if (reached) 0.4f else 1f
            isEnabled = !reached
        }
        binding.btnMoveDown.apply {
            val reached = iv.translationY >= maxY
            alpha = if (reached) 0.4f else 1f
            isEnabled = !reached
        }


    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap(1000) { confirmExit() }
                btnResetCustomize.tap(1000) { handleReset() }
                btnFlip.tap(1000) { viewModel.setIsFlip() }
                binding.actionBar.btnActionBarRightText.tap(1000) {
                    viewModel.checkDataInternet(this@CustomizeCharacterActivity) { handleSave() }
                }
            }
            btnMove.tap(1000) {
                if (layoutMove.isVisible) {
                    layoutMove.invisible()
                    rcvLayer.visible()
                    flBottomNav.visible()
                    updateMoveButtonState()
                    return@tap
                }

                if (!btnMove.isEnabled) return@tap

                layoutMove.visible()
                rcvLayer.invisible()
                flBottomNav.invisible()
                btnMove.setImageResource(R.drawable.ic_move_slt)
                updateScaleButtons()
            }
            //  btnRandom.tap { viewModel.checkDataInternet(this@CustomizeCharacterActivity) { handleRandomAllLayer() } }
            // Temporarily hidden by product request. Keep color strip visible without the toggle button.
            // btnColor.tap(1000) { viewModel.checkDataInternet(this@CustomizeCharacterActivity) { handleStatusColor() } }
            // btnHide.tap(1000) { viewModel.checkDataInternet(this@CustomizeCharacterActivity) { viewModel.setIsHideView() } }

            val STEP = 5f
            val ROTATE_STEP = 15f
            val container = binding.layoutCustomLayer

            SeekBar.max = 100
            updateScaleButtons()

            fun updateScaleFromTouch(event: MotionEvent): Boolean {
                if (SeekBar.height <= 0) return true
                if (event.action != MotionEvent.ACTION_DOWN &&
                    event.action != MotionEvent.ACTION_MOVE &&
                    event.action != MotionEvent.ACTION_UP
                ) {
                    return true
                }

                val seekBarLocation = IntArray(2)
                SeekBar.getLocationOnScreen(seekBarLocation)
                val yInSeekBar = (event.rawY - seekBarLocation[1])
                    .coerceIn(0f, SeekBar.height.toFloat())
                val progress = ((1f - yInSeekBar / SeekBar.height) * SeekBar.max)
                    .roundToInt()
                    .coerceIn(0, SeekBar.max)
                SeekBar.progress = progress
                return true
            }

            SeekBar.setOnTouchListener { _, event -> updateScaleFromTouch(event) }
            IcThumb.setOnTouchListener { _, event -> updateScaleFromTouch(event) }

            SeekBar.setOnSeekBarChangeListener(object :
                android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (viewModel.positionCustom !in viewModel.imageViewList.indices) return
                    val iv = viewModel.imageViewList[viewModel.positionCustom]
                    val scale = progressToScale(progress)

                    iv.scaleX = scale
                    iv.scaleY = scale

                    viewModel.layerTransformList[viewModel.positionCustom].scaleX = scale
                    viewModel.layerTransformList[viewModel.positionCustom].scaleY = scale

                    updateIcThumb(progress)
                    updateResetBtn()
                    updateMoveButtons()
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })


            btnMoveLeft.tapAndHold {
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                // val maxX = (container.width - iv.width) / 2f
                val maxX = container.width.toFloat() / 2f

                iv.translationX = (iv.translationX - STEP).coerceIn(-maxX, maxX)
                viewModel.layerTransformList[viewModel.positionCustom].translationX =
                    iv.translationX
                updateResetBtn()
                updateMoveButtons()

            }
            btnMoveRight.tapAndHold {
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                //val maxX = (container.width - iv.width) / 2f
                val maxX = container.width.toFloat() / 2f

                iv.translationX = (iv.translationX + STEP).coerceIn(-maxX, maxX)

                viewModel.layerTransformList[viewModel.positionCustom].translationX =
                    iv.translationX

                updateResetBtn()
                updateMoveButtons()

            }

            btnMoveUp.tapAndHold {
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                val maxY = container.height.toFloat() / 2f

                //val maxY = (container.height - iv.height) / 2f
                iv.translationY = (iv.translationY - STEP).coerceIn(-maxY, maxY)

                viewModel.layerTransformList[viewModel.positionCustom].translationY =
                    iv.translationY
                updateResetBtn()
                updateMoveButtons()

            }

            btnMoveDown.tapAndHold {
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                // val maxY = (container.height - iv.height) / 2f
                val maxY = container.height.toFloat() / 2f

                iv.translationY = (iv.translationY + STEP).coerceIn(-maxY, maxY)

                viewModel.layerTransformList[viewModel.positionCustom].translationY =
                    iv.translationY

                updateResetBtn()
                updateMoveButtons()


            }

            rotateToLeft.tapAndHold {
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                iv.rotation -= ROTATE_STEP
                viewModel.layerTransformList[viewModel.positionCustom].rotation = iv.rotation

                updateResetBtn()

            }

            rotateToRight.tapAndHold {
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                iv.rotation += ROTATE_STEP
                viewModel.layerTransformList[viewModel.positionCustom].rotation = iv.rotation
                updateResetBtn()

            }

            btnResetMove.tap {
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                iv.translationX = 0f
                iv.translationY = 0f
                iv.rotation = 0f
                iv.scaleY = 1f
                iv.scaleX = 1f
                viewModel.resetLayerTransform(viewModel.positionCustom)
                updateResetBtn()
                updateMoveButtons()
                updateScaleButtons()
            }


        }
        handleRcv()
    }

    fun progressToScale(progress: Int): Float {
        return MIN_SCALE + (progress / 100f) * (MAX_SCALE - MIN_SCALE)
    }

    fun scaleToProgress(scale: Float): Int {
        return (((scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE)) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    fun updateIcThumb(progress: Int) {
        binding.SeekBar.post {
            val range = binding.SeekBar.height - binding.IcThumb.height
            binding.IcThumb.translationY = (0.5f - progress / 100f) * range
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            btnActionBarRightText.visible()
            tvRightText.visible()
            tvRightText.setText(R.string.next)
            btnActionBarCenter.visible()
            tvRightText.isSelected = true
        }
        binding.btnFlip.visible()

    }

    private fun initRcv() {
        binding.apply {
            rcvLayer.apply {
                adapter = layerCustomizeAdapter
                itemAnimator = null
            }

            rcvColor.apply {
                adapter = colorLayerCustomizeAdapter
                itemAnimator = null
            }

            rcvNavigation.apply {
                adapter = bottomNavigationCustomizeAdapter
                itemAnimator = null
            }
        }
    }

    private fun scrollSelectedColorIntoView(retryCount: Int = 0) {
        val recyclerView = binding.rcvColor
        val colorList =
            viewModel.colorItemNavList.getOrNull(viewModel.positionNavSelected).orEmpty()
        val selectedPosition = colorList.indexOfFirst { it.isSelected }
        if (selectedPosition == -1) return

        recyclerView.post {
            if (isFinishing || isDestroyed) return@post

            val layoutManager = recyclerView.layoutManager
            val recyclerWidth = recyclerView.width
            if (recyclerWidth <= 0) {
                if (retryCount < 3) {
                    recyclerView.postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            scrollSelectedColorIntoView(retryCount + 1)
                        }
                    }, 50)
                }
                return@post
            }

            if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
                val selectedView = layoutManager.findViewByPosition(selectedPosition)
                val itemWidth = selectedView?.width
                    ?: (32 * recyclerView.resources.displayMetrics.density).toInt()
                val offset = ((recyclerWidth - itemWidth) / 2).coerceAtLeast(0)
                layoutManager.scrollToPositionWithOffset(selectedPosition, offset)
            } else {
                recyclerView.scrollToPosition(selectedPosition)
            }
        }
    }

    private fun updateMoveButtonState() {
        val isNone = viewModel.keySelectedItemList
            .getOrNull(viewModel.positionNavSelected)
            .isNullOrEmpty()
        binding.btnMove.apply {
            if (binding.layoutMove.isVisible) {
                isEnabled = true

                setImageResource(R.drawable.ic_move_slt)
            } else {
                isEnabled = !isNone
                setImageResource(if (isNone)
                    R.drawable.ic_move_uslt else
                    R.drawable.ic_move)
            }
        }
    }

    private fun handleRcv() {
        layerCustomizeAdapter.onItemClick =
            { item, position ->
                viewModel.checkDataInternet(this) {
                    handleFillLayer(
                        item,
                        position
                    )
                }
            }

        layerCustomizeAdapter.onNoneClick =
            { position ->
                viewModel.checkDataInternet(this) {
                    handleNoneLayer(position)
                    updateMoveButtonState()
                }
            }

        layerCustomizeAdapter.onRandomClick =
            { viewModel.checkDataInternet(this) { handleRandomLayer() } }

        colorLayerCustomizeAdapter.onItemClick =
            { position -> viewModel.checkDataInternet(this) { handleChangeColorLayer(position) } }

        bottomNavigationCustomizeAdapter.onItemClick =
            { positionBottomNavigation ->
                viewModel.checkDataInternet(this) {
                    handleClickBottomNavigation(
                        positionBottomNavigation
                    )
                }
            }
    }

    private fun initData() {
        val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
            eLog("initData: ${throwable.message}")
            CoroutineScope(Dispatchers.Main).launch {
                dismissLoading()
                hideNavigation(true)

                val dialogExit =
                    YesNoDialog(
                        this@CustomizeCharacterActivity,
                        R.string.error,
                        R.string.an_error_occurred
                    )
                dialogExit.show()
                dialogExit.onNoClick = {
                    dialogExit.dismiss()
                    finish()
                }
                dialogExit.onYesClick = {
                    dialogExit.dismiss()
                    hideNavigation(false)

                    startIntentRightToLeft(
                        CustomizeCharacterActivity::class.java,
                        viewModel.positionSelected
                    )
                    finish()
                }
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO + handleExceptionCoroutine).launch {
            var pathImageDefault = ""
            // Get data from list
            val deferred1 = async {
                viewModel.updateAvatarPath(viewModel.dataCustomize.value!!.avatar)
                when (viewModel.statusFrom) {
                    ValueKey.CREATE -> {
                        viewModel.resetDataList()
                        viewModel.addValueToItemNavList()
                        viewModel.setItemColorDefault()
                        viewModel.setFocusItemNavDefault()
                    }

                    // Edit
                    else -> {
                        viewModel.updateSuggestionModel(
                            MediaHelper.readModelFromFile<SuggestionModel>(
                                this@CustomizeCharacterActivity,
                                ValueKey.SUGGESTION_FILE_INTERNAL
                            )!!
                        )
                        viewModel.fillSuggestionToCustomize()
                    }
                }

                viewModel.setPositionCustom(viewModel.dataCustomize.value!!.layerList.first().positionCustom)
                viewModel.setPositionNavSelected(viewModel.dataCustomize.value!!.layerList.first().positionNavigation)
                viewModel.setBottomNavigationListDefault()
                dLog("deferred1")
                return@async true
            }
            // Add custom view in FrameLayout
            val deferred2 = async(Dispatchers.Main) {
                if (deferred1.await()) {
                    viewModel.setImageViewList(binding.layoutCustomLayer)
                    if (viewModel.layerTransformList.size != viewModel.imageViewList.size) {
                        viewModel.initLayerTransformList(viewModel.imageViewList.size)
                    }
                    dLog("deferred2")
                }
                return@async true
            }

            // Fill data default
            val deferred3 = async {
                if (deferred1.await() && deferred2.await()) {
                    if (viewModel.statusFrom == ValueKey.CREATE) {
                        pathImageDefault =
                            viewModel.dataCustomize.value!!.layerList.first().layer.first().image
                        viewModel.setIsSelectedItem(viewModel.positionCustom)
                        viewModel.setPathSelected(viewModel.positionCustom, pathImageDefault)
                        viewModel.setKeySelected(viewModel.positionNavSelected, pathImageDefault)
                    }
                    dLog("deferred3")
                }
                return@async true
            }

            withContext(Dispatchers.Main) {
                if (deferred1.await() && deferred2.await() && deferred3.await()) {
                    when (viewModel.statusFrom) {
                        ValueKey.CREATE -> {
                            Glide.with(this@CustomizeCharacterActivity).load(pathImageDefault)
                                .override(512)
                                .encodeQuality(70)
                                .into(viewModel.imageViewList[viewModel.positionCustom])
                        }

                        // Edit
                        else -> {
                            viewModel.pathSelectedList.forEachIndexed { index, path ->
                                if (path != "") {
                                    Glide.with(this@CustomizeCharacterActivity).load(path)
                                        .override(512)
                                        .encodeQuality(70)
                                        .into(viewModel.imageViewList[index])
                                }
                            }
                            viewModel.layerTransformList.forEachIndexed { index, transform ->
                                val iv = viewModel.imageViewList[index]
                                iv.translationX = transform.translationX
                                iv.translationY = transform.translationY
                                iv.rotation = transform.rotation
                                iv.scaleX = transform.scaleX
                                iv.scaleY = transform.scaleY
                            }
                        }
                    }

                    layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                    colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                    checkStatusColor()
                    scrollSelectedColorIntoView()
                    // Apply flip state after imageViewList is populated (collector fires too early when list is still empty)
                    val flipRotation = if (viewModel.isFlip.value) -180f else 0f
                    viewModel.imageViewList.forEach { it.rotationY = flipRotation }
                    viewModel.setIsCreated(true)
                    dismissLoading()
                    delay(300)
                    dismissLoading()
                    hideNavigation(true)
                    dLog("main")
                }

                // if(viewModel.dataCustomize.value?.dataName =="data2") binding.btnRandom.invisible()
            }
        }
    }

    private fun checkStatusColor() {
        if (viewModel.colorItemNavList[viewModel.positionNavSelected].isNotEmpty()) {
            binding.color.visible()
            binding.btnColor.gone()
            binding.flColor.visible()
        } else {
            binding.color.invisible()
            binding.btnColor.gone()
            binding.flColor.invisible()
        }
    }

    private fun handleStatusColor(isClose: Boolean = false) {
        if (isClose) {
            binding.flColor.invisible()
            viewModel.updateIsShowColorList(viewModel.positionNavSelected, false)
        } else {
            if (viewModel.isShowColorList[viewModel.positionNavSelected]) {
                binding.flColor.invisible()
            } else {
                binding.flColor.visible()
            }
            viewModel.updateIsShowColorList(
                viewModel.positionNavSelected,
                !viewModel.isShowColorList[viewModel.positionNavSelected]
            )
        }
        checkStatusColor()
    }

    private fun handleFillLayer(item: ItemNavCustomModel, position: Int) {
        lastClickedLayerPosition = position // Save clicked position for scrolling
        android.util.Log.d("CustomizeScroll", "Layer clicked at position: $position")
        android.util.Log.d(
            "LayerClick",
            "path=${item.path} | thumb=${item.thumb} | position=$position"
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val pathSelected = viewModel.setClickFillLayer(item, position)
            android.util.Log.d("LayerClick", "pathSelected=$pathSelected")
            withContext(Dispatchers.Main) {
                Glide.with(this@CustomizeCharacterActivity).load(pathSelected)
                    .override(Target.SIZE_ORIGINAL)

                    .into(viewModel.imageViewList[viewModel.positionCustom])
                layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
            }
        }
    }

    private fun handleNoneLayer(position: Int) {
        lastClickedLayerPosition = position // Save clicked position for scrolling
        android.util.Log.d("CustomizeScroll", "None layer clicked at position: $position")

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.setIsSelectedItem(viewModel.positionCustom)
            viewModel.setPathSelected(viewModel.positionCustom, "")
            viewModel.setKeySelected(viewModel.positionNavSelected, "")
            viewModel.setItemNavList(viewModel.positionNavSelected, position)
            withContext(Dispatchers.Main) {
                Glide.with(this@CustomizeCharacterActivity)
                    .clear(viewModel.imageViewList[viewModel.positionCustom])
                layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
            }
        }
    }

    private fun handleRandomLayer() {
        lifecycleScope.launch(Dispatchers.IO) {
            val (pathRandom, isMoreColors) = viewModel.setClickRandomLayer()
            withContext(Dispatchers.Main) {
                Glide.with(this@CustomizeCharacterActivity).load(pathRandom)
                    .override(Target.SIZE_ORIGINAL)
                    .into(viewModel.imageViewList[viewModel.positionCustom])
                layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                if (isMoreColors) {
                    colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                }
            }
        }
    }

    private fun handleChangeColorLayer(position: Int) {
        android.util.Log.d(
            "ColorClick",
            "=== handleChangeColorLayer === position from adapter: $position"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Lấy path màu mới cho item đang được chọn
            val pathColor = viewModel.setClickChangeColor(position)
            android.util.Log.d("ColorClick", "pathColor result: $pathColor")

            // 2. ⭐ Update màu cho TẤT CẢ items trong rcvLayer
            viewModel.updateAllItemsColor(position)

            withContext(Dispatchers.Main) {
                // 3. Update ảnh trong canvas chính (layoutCustomLayer)
                if (pathColor != "") {
                    Glide.with(this@CustomizeCharacterActivity)
                        .load(pathColor)
                        .override(Target.SIZE_ORIGINAL)
                        .into(viewModel.imageViewList[viewModel.positionCustom])

                }

                // 4. Update highlight trong rcvColor
                colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])

                // 5. ⭐ Refresh rcvLayer với data mới (tất cả items đã đổi màu)
                // Sử dụng .toList() để tạo list mới, giúp DiffUtil detect changes
                val newList = viewModel.itemNavList[viewModel.positionNavSelected].toList()
                android.util.Log.d(
                    "CustomizeScroll",
                    "submitList called - list size: ${newList.size}"
                )

                layerCustomizeAdapter.submitList(newList) {
                    android.util.Log.d("CustomizeScroll", "submitList callback - list committed")

                    // 6. ⭐ Scroll rcvLayer to center the last clicked layer position IMMEDIATELY
                    if (lastClickedLayerPosition >= 0) {
                        val layoutManager = binding.rcvLayer.layoutManager

                        if (layoutManager is androidx.recyclerview.widget.GridLayoutManager) {
                            val spanCount = layoutManager.spanCount

                            // Calculate row position
                            val rowPosition = (lastClickedLayerPosition / spanCount) * spanCount

                            // Calculate offset to center the row on screen
                            val recyclerHeight = binding.rcvLayer.height

                            // Use estimated item height if view not yet laid out
                            val itemView =
                                layoutManager.findViewByPosition(lastClickedLayerPosition)
                            val itemHeight =
                                itemView?.height ?: (recyclerHeight / 5) // Estimate ~1/5 of screen

                            // Center the item vertically: (recyclerHeight / 2) - (itemHeight / 2)
                            val centerOffset = (recyclerHeight / 2) - (itemHeight / 2)

                            android.util.Log.d(
                                "CustomizeScroll",
                                "INSTANT scroll to position $lastClickedLayerPosition - row: $rowPosition, offset: $centerOffset"
                            )

                            // Immediate scroll without animation
                            layoutManager.scrollToPositionWithOffset(rowPosition, centerOffset)
                        } else {
                            // Fallback - but shouldn't happen
                            binding.rcvLayer.scrollToPosition(lastClickedLayerPosition)
                        }
                    }
                }
            }
        }
    }

    private fun handleClickBottomNavigation(positionBottomNavigation: Int) {
        if (positionBottomNavigation == viewModel.positionNavSelected) return
        if (binding.layoutMove.isVisible) {
            binding.layoutMove.invisible()
            binding.rcvLayer.visible()
            binding.flBottomNav.visible()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.setPositionNavSelected(positionBottomNavigation)
            viewModel.setPositionCustom(viewModel.dataCustomize.value!!.layerList[positionBottomNavigation].positionCustom)
            viewModel.setClickBottomNavigation(positionBottomNavigation)
            withContext(Dispatchers.Main) {

                val transform = viewModel.layerTransformList[viewModel.positionCustom]
                val iv = viewModel.imageViewList[viewModel.positionCustom]
                iv.translationX = transform.translationX
                iv.translationY = transform.translationY
                iv.rotation = transform.rotation
                iv.scaleX = transform.scaleX
                iv.scaleY = transform.scaleY
                scrollSelectedColorIntoView()
                checkStatusColor()
                updateResetBtn()
                updateMoveButtons()
                updateScaleButtons()
                updateMoveButtonState()

            }
        }
    }

    private fun confirmExit() {
        val dialog =
            YesNoDialog(
                this, R.string.exit, R.string.do_you_want_to_exit, isError = false,
                dialogType = DialogType.DELETE_EXIT
            )
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onYesClick = {
            dialog.dismiss()
            showInterAll { finish() }
        }
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation(false)
        }
    }

    private fun handleSave() {
        if (isSavingCharacter || isOpeningAddCharacter || isFinishing || isDestroyed) return
        isSavingCharacter = true
        binding.actionBar.btnActionBarRightText.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.saveImageFromView(this@CustomizeCharacterActivity, binding.layoutCustomLayer)
                .collect { result ->
                    when (result) {
                        is SaveState.Loading -> showLoading()

                        is SaveState.Error -> {
                            dismissLoading()
                            withContext(Dispatchers.Main) {
                                isSavingCharacter = false
                                binding.actionBar.btnActionBarRightText.isEnabled = true
                                showToast(R.string.save_failed_please_try_again)
                            }
                        }

                        is SaveState.Success -> {
                            dismissLoading()
                            when (viewModel.statusFrom) {
                                ValueKey.EDIT -> {
                                    // Chạy ngầm trong background, không blocking UI
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        viewModel.updateEditCharacter(
                                            this@CustomizeCharacterActivity,
                                            result.path
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        logEvent("click_item_${viewModel.positionSelected}_edit")
                                        // ✅ 1) GỬI RESULT VỀ ViewActivity NGAY (nhưng chưa finish)
                                        val data = android.content.Intent().apply {
                                            putExtra("NEW_PATH", result.path)
                                        }
                                        setResult(RESULT_OK, data)

                                        // ✅ 2) VẪN SANG AddCharacterActivity như bạn muốn
                                        viewModel.checkDataInternet(this@CustomizeCharacterActivity) {
                                            openAddCharacterOnce(result.path)
                                        }
                                    }
                                }

                                else -> {
                                    // Chạy ngầm trong background, không blocking UI
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        viewModel.addCharacterToEditList(
                                            this@CustomizeCharacterActivity,
                                            result.path
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        logEvent("click_item_${viewModel.positionSelected}_done")
                                        viewModel.checkDataInternet(this@CustomizeCharacterActivity) {
                                            openAddCharacterOnce(result.path)
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
        }
    }

    private fun openAddCharacterOnce(path: String) {
        if (isOpeningAddCharacter || isFinishing || isDestroyed) return
        if (!canOpenAddCharacterNow()) return
        isOpeningAddCharacter = true
        binding.actionBar.btnActionBarRightText.isEnabled = false


            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, AddCharacterActivity::class.java).apply {
                    putExtra(IntentKey.INTENT_KEY, path)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val option = android.app.ActivityOptions.makeCustomAnimation(
                    this,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                startActivity(intent, option.toBundle())
            }

    }

    private fun handleReset() {
        val dialog = YesNoDialog(
            this@CustomizeCharacterActivity,
            R.string.reset,
            R.string.change_your_whole_design_are_you_sure,
            dialogType = DialogType.RESET
        )
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onYesClick = {
            dialog.dismiss()
            lifecycleScope.launch(Dispatchers.IO) {
                val pathDefault = viewModel.setClickReset()
                withContext(Dispatchers.Main) {
                    viewModel.imageViewList.forEach { imageView ->
                        Glide.with(this@CustomizeCharacterActivity).clear(imageView)
                    }
                    Glide.with(this@CustomizeCharacterActivity).load(pathDefault)
                        .override(Target.SIZE_ORIGINAL)
                        .into(viewModel.imageViewList[viewModel.dataCustomize.value!!.layerList.first().positionCustom])
                    layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                    colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                    showInterAll { hideNavigation(false) }

                    viewModel.imageViewList.forEach { iv ->
                        iv.translationX = 0f
                        iv.translationY = 0f
                        iv.rotation = 0f
                        iv.scaleX = 1f
                        iv.scaleY = 1f

                    }
                    viewModel.initLayerTransformList(viewModel.imageViewList.size)
                    updateResetBtn()
                    updateMoveButtonState()
                }
            }
        }
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation(false)
        }
    }

    private fun handleRandomAllLayer() {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                binding.actionBar.btnActionBarRightText.isEnabled = false
            }
            val timeStart = System.currentTimeMillis()
            val isOutTurn = viewModel.setClickRandomFullLayer()

            withContext(Dispatchers.Main) {
                viewModel.pathSelectedList.forEachIndexed { index, path ->
                    Glide.with(this@CustomizeCharacterActivity)
                        .load(path)
                        .override(Target.SIZE_ORIGINAL)
                        .into(viewModel.imageViewList[index])
                }
                layerCustomizeAdapter.submitList(viewModel.itemNavList[viewModel.positionNavSelected])
                colorLayerCustomizeAdapter.submitList(viewModel.colorItemNavList[viewModel.positionNavSelected])
                if (isOutTurn) binding.btnRandom.invisible()
                val timeEnd = System.currentTimeMillis()
                delay(800)
                binding.actionBar.btnActionBarRightText.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setIsCreated(false)
    }

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {
        confirmExit()
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(
            this, getString(R.string.native_collap_maker),
            binding.flNativeCollab
        )
    }

    override fun initAds() {
        initNativeCollab()
    }

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()

    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("CustomizeLifecycle", "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        isSavingCharacter = false
        isOpeningAddCharacter = false
        binding.actionBar.btnActionBarRightText.isEnabled = true
        android.util.Log.d("CustomizeLifecycle", "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("CustomizeLifecycle", "onPause() called")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyUiCustomize()
            hideNavigation(true)

            window.decorView.removeCallbacks(reHideRunnable)
            window.decorView.postDelayed(reHideRunnable, 1500)
        } else {
            window.decorView.removeCallbacks(reHideRunnable)
        }
    }

    private val reHideRunnable = Runnable {
        applyUiCustomize()
        hideNavigation(true)
    }

    @Suppress("DEPRECATION")
    private fun applyUiCustomize() {
        // Cho phép app tự vẽ màu system bar
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // Transparent status bar
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Flags
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        // nếu muốn icon status bar đen thì thêm:
        // or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}

package com.piratemaker.postermaker.poster.activity_app.add_character

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.facebook.shimmer.ShimmerDrawable
import com.lvt.ads.util.Admob
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.checkPermissions
import com.piratemaker.postermaker.poster.core.extensions.goToSettings
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.hideNavigation
import com.piratemaker.postermaker.poster.core.extensions.hideSoftKeyboard
import com.piratemaker.postermaker.poster.core.extensions.invisible
import com.piratemaker.postermaker.poster.core.extensions.loadImage
import com.piratemaker.postermaker.poster.core.extensions.loadNativeCollabAds
import com.piratemaker.postermaker.poster.core.extensions.requestPermission
import com.piratemaker.postermaker.poster.core.extensions.select
import com.piratemaker.postermaker.poster.core.extensions.setFont
import com.piratemaker.postermaker.poster.core.extensions.setImageActionBar
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.showToast
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.custom.text.DoubleStrokeTextView
import com.piratemaker.postermaker.poster.core.helper.BitmapHelper
import com.piratemaker.postermaker.poster.core.helper.LanguageHelper
import com.piratemaker.postermaker.poster.core.helper.UnitHelper
import com.piratemaker.postermaker.poster.core.utils.DataLocal
import com.piratemaker.postermaker.poster.core.utils.key.IntentKey
import com.piratemaker.postermaker.poster.core.utils.key.RequestKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.SaveState
import com.piratemaker.postermaker.poster.core.draw.Draw
import com.piratemaker.postermaker.poster.core.draw.DrawableDraw
import com.piratemaker.postermaker.poster.databinding.ActivityAddCharacterBinding
import com.piratemaker.postermaker.poster.dialog.ChooseColorDialog
import com.piratemaker.postermaker.poster.dialog.DialogSpeech
import com.piratemaker.postermaker.poster.dialog.DialogType
import com.piratemaker.postermaker.poster.dialog.YesNoDialog
import com.piratemaker.postermaker.poster.core.listener.listenerdraw.OnDrawListener
import com.piratemaker.postermaker.poster.activity_app.add_character.adapter.BackgroundColorAdapter
import com.piratemaker.postermaker.poster.activity_app.add_character.adapter.BackgroundImageAdapter
import com.piratemaker.postermaker.poster.activity_app.add_character.adapter.StickerAdapter
import com.piratemaker.postermaker.poster.activity_app.add_character.adapter.TextColorAdapter
import com.piratemaker.postermaker.poster.activity_app.add_character.adapter.TextFontAdapter
import com.piratemaker.postermaker.poster.activity_app.permission.PermissionViewModel
import com.piratemaker.postermaker.poster.activity_app.maker_view.ViewActivity
import com.piratemaker.postermaker.poster.activity_app.maker_success.SuccessActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.collections.get
import kotlin.getValue
import kotlin.toString

class AddCharacterActivity : BaseActivity<ActivityAddCharacterBinding>() {
    private val viewModel: AddCharacterViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private val backgroundImageAdapter by lazy { BackgroundImageAdapter() }
    private val backgroundColorAdapter by lazy { BackgroundColorAdapter() }
    private val stickerAdapter by lazy { StickerAdapter() }
    private val speechAdapter by lazy { StickerAdapter() }
    private val textFontAdapter by lazy { TextFontAdapter(this) }
    private val textColorAdapter by lazy { TextColorAdapter() }
    private var exitDialog: YesNoDialog? = null
    private var isExiting = false

    // Photo Picker - NO PERMISSION REQUIRED
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                handleSetBackgroundImage(uri.toString(), 0)
            }
        }

    override fun onResume() {
        super.onResume()
        hideNavigation(true)
    }

    private val buttonNavigationList by lazy {
        arrayListOf(
            binding.bottomTabs.btnBackground,
            binding.bottomTabs.btnSticker,
            binding.bottomTabs.btnSpeech,
            binding.bottomTabs.btnText,
        )
    }

    private val layoutNavigationList by lazy {
        arrayListOf(
            binding.functionPanels.backgroundPanel.lnlBackground,
            binding.functionPanels.stickerPanel.lnlSticker,
            binding.functionPanels.speechPanel.lnlSpeech,
            binding.functionPanels.textPanel.lnlText,
        )
    }

    override fun setViewBinding(): ActivityAddCharacterBinding {
        return ActivityAddCharacterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        viewModel.layoutParams = binding.flFunction.layoutParams as ViewGroup.MarginLayoutParams
        viewModel.originalMarginBottom =
            viewModel.layoutParams.topMargin  // Capture initial topMargin
        initRcv()
        initDrawView()
        initData()
        setupKeyboardLogging()
        setupBackPressHandler()

        // 🔒 FIX CỨNG VIVO ANDROID 8 AUTO FOCUS
        binding.main.post {
            binding.main.requestFocus()     // Cướp focus khỏi EditText
            binding.functionPanels.textPanel.edtText.clearFocus()    // Đảm bảo functionPanels.textPanel.edtText không focus
            hideSoftKeyboard()              // Ép keyboard tắt
            viewModel.setIsFocusEditText(false) // Reset state
        }
    }

    private var lastImeVisible = false

    private fun setupKeyboardLogging() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            Log.d("EditTextFlow", "WindowInsets: imeVisible=$imeVisible, imeHeight=$imeHeight")
            Log.d(
                "EditTextFlow",
                "functionPanels.textPanel.scvText.scrollY=${binding.functionPanels.textPanel.scvText.scrollY}, functionPanels.textPanel.scvText.canScrollVertically(1)=${
                    binding.functionPanels.textPanel.scvText.canScrollVertically(1)
                }, functionPanels.textPanel.scvText.canScrollVertically(-1)=${binding.functionPanels.textPanel.scvText.canScrollVertically(-1)}"
            )

            // Handle keyboard visibility changes
            if (lastImeVisible && !imeVisible) {
                // Keyboard was visible, now it's hidden
                Log.d("EditTextFlow", "Keyboard dismissed by system (via window insets)")
                if (viewModel.isFocusEditText.value && binding.functionPanels.textPanel.edtText.hasFocus()) {
                    Log.d(
                        "EditTextFlow",
                        "EditText still has focus - back button dismissed keyboard"
                    )
                    // Back button was pressed while EditText had focus
                    viewModel.setIsFocusEditText(false)
                }
            }
            lastImeVisible = imeVisible

            insets
        }
    }

    private fun setupBackPressHandler() {
        val callback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("EditTextFlow", "OnBackPressedCallback.handleOnBackPressed called")
                Log.d("EditTextFlow", "isFocusEditText.value=${viewModel.isFocusEditText.value}")
                Log.d("EditTextFlow", "functionPanels.textPanel.edtText.hasFocus()=${binding.functionPanels.textPanel.edtText.hasFocus()}")

                if (viewModel.isFocusEditText.value || binding.functionPanels.textPanel.edtText.hasFocus()) {
                    Log.d(
                        "EditTextFlow",
                        "Back pressed with EditText focused - hiding keyboard via callback"
                    )
                    viewModel.setIsFocusEditText(false)
                } else {
                    Log.d(
                        "EditTextFlow",
                        "Back pressed without EditText focused - showing confirmExit"
                    )
                    confirmExit()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        Log.d("EditTextFlow", "OnBackPressedCallback registered, enabled=${callback.isEnabled}")
    }
//    private fun setupKeyboardDetection() {
//        binding.main.viewTreeObserver.addOnGlobalLayoutListener {
//            val rect = android.graphics.Rect()
//            binding.main.getWindowVisibleDisplayFrame(rect)
//
//            val screenHeight = binding.main.rootView.height
//            val keypadHeight = screenHeight - rect.bottom
//
//            // If keyboard height is more than 15% of screen, keyboard is showing
//            val isKeyboardNowShowing = keypadHeight > screenHeight * 0.15
//
//            if (isKeyboardNowShowing != isKeyboardShowing) {
//                isKeyboardShowing = isKeyboardNowShowing
//
//                if (isKeyboardShowing && binding.functionPanels.textPanel.edtText.hasFocus()) {
//                    // Keyboard just appeared
//                    adjustLayoutForKeyboard(keypadHeight)
//                } else if (!isKeyboardShowing) {
//                    // Keyboard just disappeared
//                    resetLayout()
//                }
//            }
//        }
//    }
//    private fun adjustLayoutForKeyboard(keyboardHeight: Int) {
//        binding.functionPanels.textPanel.scvText.postDelayed({
//            // Scroll the ScrollView to show EditText
//            val location = IntArray(2)
//            binding.functionPanels.textPanel.edtText.getLocationOnScreen(location)
//            val edtY = location[1]
//            val edtBottom = edtY + binding.functionPanels.textPanel.edtText.height
//
//            val screenHeight = resources.displayMetrics.heightPixels
//            val visibleHeight = screenHeight - keyboardHeight
//
//            if (edtBottom > visibleHeight) {
//                // Calculate how much to scroll
//                val scrollAmount = edtBottom - visibleHeight + UnitHelper.dpToPx(this, 50)
//                binding.functionPanels.textPanel.scvText.smoothScrollBy(0, scrollAmount)
//            }
//        }, 100)
//    }
//
//    private fun resetLayout() {
//        binding.functionPanels.textPanel.scvText.smoothScrollTo(0, 0)
//    }


    override fun dataObservable() {
        binding.apply {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
//                        typeNavigation
                        viewModel.typeNavigation.collect { type ->
                            if (type != -1) {
                                setupTypeNavigation(type)
                            }
                        }
                    }

                    launch {
//                        typeBackground
                        viewModel.typeBackground.collect { type ->
                            if (type != -1) {
                                setupTypeBackground(type)
                            }
                        }
                    }

                    launch {
//                        isFocusEditText
                        viewModel.isFocusEditText.collect { status ->
                            Log.d("EditTextFlow", "isFocusEditText.collect: status=$status")
                            if (status) {

                                // Clear FLAG_LAYOUT_NO_LIMITS to allow adjustResize to work
                                Log.d(
                                    "EditTextFlow",
                                    "Keyboard showing - clearing FLAG_LAYOUT_NO_LIMITS"
                                )
                                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                                viewModel.layoutParams.topMargin =
                                    UnitHelper.dpToPx(this@AddCharacterActivity, -160)
                                flFunction.layoutParams = viewModel.layoutParams
                                Log.d(
                                    "EditTextFlow",
                                    "Layout adjusted - topMargin: ${viewModel.layoutParams.topMargin}"
                                )
                            } else {
                                // Scroll back to top
                                // 🔒 CHỈ reset layout KHI IME ĐÃ THẬT SỰ TẮT
                                functionPanels.textPanel.scvText.smoothScrollTo(0, 0)
                                viewModel.layoutParams.topMargin = viewModel.originalMarginBottom
                                flFunction.layoutParams = viewModel.layoutParams

                                hideSoftKeyboard()

                                functionPanels.textPanel.edtText.clearFocus()

                                hideSoftKeyboard()
                                hideNavigation(true)
                                delay(300)
                                hideSoftKeyboard()


                                // Keyboard vẫn mở → ÉP TẮT

                                Log.d(
                                    "EditTextFlow",
                                    "Layout reset complete - topMargin: ${viewModel.layoutParams.topMargin}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap(1000) { confirmExit() }
                btnActionBarCenter.tap(1000) { confirmReset() }
                btnActionBarRight.tap(1000) {
                    handleSave()
                }
            }
            functionPanels.backgroundPanel.btnBackgroundImage.tap(1000) { viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND) }
            functionPanels.backgroundPanel.btnBackgroundColor.tap(1000) { viewModel.setTypeBackground(ValueKey.COLOR_BACKGROUND) }
            bottomTabs.btnBackground.tap(1000) { viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION) }
            bottomTabs.btnSticker.tap(1000) { viewModel.setTypeNavigation(ValueKey.STICKER_NAVIGATION) }
            bottomTabs.btnSpeech.tap(1000) { viewModel.setTypeNavigation(ValueKey.SPEECH_NAVIGATION) }
            bottomTabs.btnText.tap(1000) { viewModel.setTypeNavigation(ValueKey.TEXT_NAVIGATION) }

            functionPanels.textPanel.edtText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    binding.tvGetText.text = p0.toString()
                }

                override fun afterTextChanged(p0: Editable?) {}
            })
            functionPanels.textPanel.edtText.setOnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    viewModel.setIsFocusEditText(false)
                    true
                } else {
                    false
                }
            }
            functionPanels.textPanel.edtText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                Log.d("EditTextFlow", "functionPanels.textPanel.edtText.onFocusChangeListener: hasFocus=$hasFocus")
                Log.d(
                    "EditTextFlow",
                    "functionPanels.textPanel.scvText.scrollY=${binding.functionPanels.textPanel.scvText.scrollY}, functionPanels.textPanel.scvText.height=${binding.functionPanels.textPanel.scvText.height}"
                )
                if (hasFocus) {
                    Log.d("EditTextFlow", "EditText gained focus - setting isFocusEditText=true")
                    viewModel.setIsFocusEditText(true)
                    // Let adjustResize handle layout - no programmatic scroll needed
                } else {
                    Log.d("EditTextFlow", "EditText lost focus - setting isFocusEditText=false")
                    // Only update if not already false (prevent recursive calls)
                    if (viewModel.isFocusEditText.value) {
                        viewModel.setIsFocusEditText(false)
                    }
                }
            }
            functionPanels.textPanel.btnDoneText.tap {
                handleDoneText()
            }
            main.tap {
                Log.d("EditTextFlow", "main layout tapped - clearing focus")
                viewModel.setIsFocusEditText(false)
                clearFocus()
            }

            backgroundImageAdapter.apply {
                onAddImageClick = {
                    viewModel.checkDataInternet(this@AddCharacterActivity, action = {
                        checkStoragePermission()
                    })
                }
                onBackgroundImageClick = { path, position ->
                    viewModel.checkDataInternet(this@AddCharacterActivity) {
                        handleSetBackgroundImage(path, position)
                    }
                }
            }

            backgroundColorAdapter.apply {
                onChooseColorClick = { handleChooseColor() }
                onBackgroundColorClick =
                    { color, position -> handleSetBackgroundColor(color, position) }
            }

            stickerAdapter.onItemClick = { path ->
                viewModel.checkDataInternet(
                    this@AddCharacterActivity,
                    action = { addDrawable(path) })
            }

            speechAdapter.onItemClick = { path -> handleSpeech(path) }

            textFontAdapter.onTextFontClick = { font, position -> handleFontClick(font, position) }

            textColorAdapter.apply {
                onChooseColorClick = { handleChooseColor(true) }
                onTextColorClick = { color, position -> handleTextColorClick(color, position) }
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setImageActionBar(btnActionBarCenter, R.drawable.ic_reset)
            btnActionBarRightText.visible()
            tvRightText.visible()
            tvRightText.setText(R.string.save)
            btnActionBarRight.visible()

            // Căn giữa nút reset vào guideline
            val params =
                btnActionBarCenter.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.endToEnd = guideline.id
            params.startToStart = guideline.id
            params.horizontalBias = 0.5f
            params.marginEnd = 0
            btnActionBarCenter.layoutParams = params
        }
    }

    override fun initText() {
        binding.apply {
            functionPanels.backgroundPanel.tvBackgroundImage.select()
            functionPanels.backgroundPanel.tvBackgroundColor.select()

            // Apply gradient to tvText, tvFont, tvColor

        }
    }

    private fun initRcv() {
        binding.apply {
            functionPanels.backgroundPanel.rcvBackgroundImage.apply {
                adapter = backgroundImageAdapter
                itemAnimator = null
            }

            functionPanels.backgroundPanel.rcvBackgroundColor.apply {
                adapter = backgroundColorAdapter
                itemAnimator = null
            }

            functionPanels.stickerPanel.rcvSticker.apply {
                adapter = stickerAdapter
                itemAnimator = null
                setItemViewCacheSize(200)
                setHasFixedSize(true)
            }

            functionPanels.speechPanel.rcvSpeech.apply {
                adapter = speechAdapter
                itemAnimator = null
                setItemViewCacheSize(200)
                setHasFixedSize(true)
            }

            functionPanels.textPanel.rcvFont.apply {
                adapter = textFontAdapter
                itemAnimator = null
            }

            functionPanels.textPanel.rcvTextColor.apply {
                adapter = textColorAdapter
                itemAnimator = null
            }
        }
    }


    fun loadCharacter(
        context: Context,
        path: String,
        imageView: ImageView,
        isLoadShimmer: Boolean = true
    ) {
        val shimmerDrawable = ShimmerDrawable().apply {
            setShimmer(DataLocal.shimmer)
        }
        if (isLoadShimmer) {
            Glide.with(context).load(path).placeholder(shimmerDrawable).error(shimmerDrawable)
                .into(imageView)
        } else {
            Glide.with(context).load(path).placeholder(shimmerDrawable).error(shimmerDrawable)
                .into(imageView)
        }

    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            showLoading()
            viewModel.loadDataDefault(this@AddCharacterActivity)
            viewModel.updatePathDefault(intent.getStringExtra(IntentKey.INTENT_KEY) ?: "")
            addDrawable(viewModel.pathDefault, true)



            withContext(Dispatchers.Main) {

                viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION)
                viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND)
                backgroundImageAdapter.submitList(viewModel.backgroundImageList.map { it.copy() })
                backgroundColorAdapter.submitList(viewModel.backgroundColorList.map { it.copy() })
                stickerAdapter.submitList(viewModel.stickerList)
                speechAdapter.submitList(viewModel.speechList)
                textFontAdapter.submitListReset(viewModel.textFontList)
                textColorAdapter.submitListReset(viewModel.textColorList)
                // Set initial text color and font to match selected items
                binding.functionPanels.textPanel.edtText.setFont(viewModel.textFontList.first().color)
                binding.functionPanels.textPanel.edtText.setTextColor(viewModel.textColorList[1].color)
                binding.tvGetText.setFont(viewModel.textFontList.first().color)
                binding.tvGetText.setTextColor(viewModel.textColorList[1].color)
                delay(200)
                binding.drawView.autoSelectFirstDraw()

                clearFocus()
                binding.drawView.autoSelectFirstDraw()
                dismissLoading(true)
            }
        }
    }

    /**
     * Async bitmap loading using Glide without blocking threads
     */
    private suspend fun loadBitmapAsync(path: String): Bitmap =
        suspendCancellableCoroutine { continuation ->
            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (continuation.isActive) {
                        continuation.resume(resource)
                    }
                }

                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("Failed to load bitmap from: $path"))
                    }
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // Cleanup if needed
                }
            }

            Glide.with(this@AddCharacterActivity)
                .asBitmap()
                .load(path)
                .into(target)

            continuation.invokeOnCancellation {
                Glide.with(this@AddCharacterActivity).clear(target)
            }
        }

    private fun addDrawable(
        path: String,
        isCharacter: Boolean = false,
        bitmapText: Bitmap? = null
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmapDefault = if (bitmapText == null) {
                    Glide.with(this@AddCharacterActivity)
                        .asBitmap()
                        .load(path)
                        .override(512, 512)
                        .encodeQuality(70)
                        .submit()
                        .get()
                } else {
                    bitmapText
                }

                val drawableEmoji = viewModel.loadDrawableEmoji(
                    this@AddCharacterActivity,
                    bitmapDefault,
                    isCharacter
                )

                withContext(Dispatchers.Main) {
                    binding.drawView.addDraw(drawableEmoji)
                }
            } catch (e: Exception) {
                Log.e("AddCharacterActivity", "Failed to add drawable: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.save_failed_please_try_again))
                }
            }
        }
    }

    private fun initDrawView() {
        binding.drawView.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onAddedDraw")
                    viewModel.updateCurrentCurrentDraw(draw)
                    viewModel.addDrawView(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onClickedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onClickedDraw")
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDeletedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDeletedDraw")
                    viewModel.deleteDrawView(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDragFinishedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDragFinishedDraw")
                    viewModel.setIsFocusEditText(false)
                }

                override fun onTouchedDownDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onTouchedDownDraw")
                    viewModel.updateCurrentCurrentDraw(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onZoomFinishedDraw(draw: Draw) {}

                override fun onFlippedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onFlippedDraw")
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDoubleTappedDraw(draw: Draw) {}

                override fun onHideOptionIconDraw() {}

                override fun onUndoDeleteDraw(draw: List<Draw?>) {}

                override fun onUndoUpdateDraw(draw: List<Draw?>) {}

                override fun onUndoDeleteAll() {}

                override fun onRedoAll() {}

                override fun onReplaceDraw(draw: Draw) {}

                override fun onEditText(draw: DrawableDraw) {}

                override fun onReplace(draw: Draw) {}
            })
        }
    }

    private fun setupTypeBackground(type: Int) {
        binding.apply {

            when (type) {
                ValueKey.IMAGE_BACKGROUND -> {
                    functionPanels.backgroundPanel.rcvBackgroundImage.visible()
                    functionPanels.backgroundPanel.rcvBackgroundColor.gone()
                    setupSelectedTabBackground(
                        functionPanels.backgroundPanel.btnBackgroundImage,
                        functionPanels.backgroundPanel.tvBackgroundImage,
                        functionPanels.backgroundPanel.imvFocusImage,
                        functionPanels.backgroundPanel.subTabImage,
                        isLeftTab = true
                    )
                    setupUnselectedTabBackground(
                        functionPanels.backgroundPanel.btnBackgroundColor,
                        functionPanels.backgroundPanel.tvBackgroundColor,
                        functionPanels.backgroundPanel.imvFocusColor,
                        functionPanels.backgroundPanel.subTabColor,
                        isLeftTab = false
                    )
                    backgroundImageAdapter.submitList(viewModel.backgroundImageList.map { it.copy() })
                }

                ValueKey.COLOR_BACKGROUND -> {
                    functionPanels.backgroundPanel.rcvBackgroundImage.gone()
                    functionPanels.backgroundPanel.rcvBackgroundColor.visible()
                    setupSelectedTabBackground(
                        functionPanels.backgroundPanel.btnBackgroundColor,
                        functionPanels.backgroundPanel.tvBackgroundColor,
                        functionPanels.backgroundPanel.imvFocusColor,
                        functionPanels.backgroundPanel.subTabColor,
                        isLeftTab = false
                    )
                    setupUnselectedTabBackground(
                        functionPanels.backgroundPanel.btnBackgroundImage,
                        functionPanels.backgroundPanel.tvBackgroundImage,
                        functionPanels.backgroundPanel.imvFocusImage,
                        functionPanels.backgroundPanel.subTabImage,
                        isLeftTab = true
                    )
                    backgroundColorAdapter.submitList(viewModel.backgroundColorList.map { it.copy() })
                }

                else -> {}
            }
        }
    }

    private fun setupSelectedTabBackground(
        tabView: View,
        textView: DoubleStrokeTextView,
        focusImage: android.widget.ImageView,
        subTab: View,
        isLeftTab: Boolean
    ) {
        // Set weight = 1.6
        val params = tabView.layoutParams as android.widget.LinearLayout.LayoutParams
        params.weight = 1.0f
        params.topMargin = 0
        tabView.layoutParams = params

        // Set text size = 18sp
        textView.textSize = 14f

        textView.setTextColor(Color.WHITE)
//        textView.setDoubleStroke(
//            outerColor = getColor(R.color.app),
//            outerWidth = resources.displayMetrics.density * 2f,
//            innerColor = Color.TRANSPARENT,
//            innerWidth = 0f
//        )

        // Show selected_tab drawable
        focusImage.setImageResource(R.drawable.bg_slt_bg)
        focusImage.scaleX = 1f
        focusImage.visible()

        // Hide subTab
        subTab.gone()
    }

    private fun setupUnselectedTabBackground(
        tabView: View,
        textView: DoubleStrokeTextView,
        focusImage: android.widget.ImageView,
        subTab: View,
        isLeftTab: Boolean
    ) {
        // Set weight = 1
        val params = tabView.layoutParams as android.widget.LinearLayout.LayoutParams
        params.weight = 1f
        params.topMargin = UnitHelper.dpToPx(this, 0f).toInt()
        tabView.layoutParams = params

        // Set text size = 14sp, color = colorPrimary
        textView.textSize = 14f

        textView.setTextColor(Color.WHITE)
//        textView.setDoubleStroke(
//            outerColor = Color.TRANSPARENT,
//            outerWidth = 0f,
//            innerColor = Color.TRANSPARENT,
//            innerWidth = 0f
//        )

        // Show un_selected_tab drawable
        focusImage.setImageResource(R.drawable.bg_uslt_bg)
        // Flip horizontally if on left side
        focusImage.scaleX = if (isLeftTab) -1f else 1f
        focusImage.visible()

        // Show subTab
        subTab.gone()
    }

    private fun setupTypeNavigation(type: Int) {
        buttonNavigationList.forEachIndexed { index, button ->
            val (res, status) = if (index == type) {
                DataLocal.bottomNavigationSelected[index] to true
            } else {
                DataLocal.bottomNavigationNotSelect[index] to false
            }

            button.setImageResource(res)
            layoutNavigationList[index].isVisible = status
        }

        // Show functionPanels.backgroundPanel.sectionTab and spaceSectionTab only when Background navigation is selected
        binding.apply {
            val isBackground = (type == ValueKey.BACKGROUND_NAVIGATION)

            // Section tab
            if (isBackground) functionPanels.backgroundPanel.sectionTab.visible() else functionPanels.backgroundPanel.sectionTab.gone()

            if (isBackground) {
                functionPanels.backgroundPanel.bgBg.visible()
                functionPanels.backgroundPanel.bgOther.gone()
            } else {
                functionPanels.backgroundPanel.bgBg.gone()
                functionPanels.backgroundPanel.bgOther.visible()
            }

        }
    }

    private fun confirmExit() {
        if (isFinishing || isDestroyed || isExiting || exitDialog?.isShowing == true) return

        val dialog = YesNoDialog(
            this, R.string.exit, R.string.do_you_want_to_exit, isError = false,
            dialogType = DialogType.DELETE_EXIT
        )
        exitDialog = dialog
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onYesClick = {
            if (!isExiting) {
                isExiting = true
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                exitDialog = null
                finish()
            }
        }
        dialog.onNoClick = {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
            exitDialog = null
            hideNavigation(true)
        }
    }

    private fun confirmReset() {
        val dialog = YesNoDialog(
            this,
            R.string.reset,
            R.string.change_your_whole_design_are_you_sure,
            dialogType = DialogType.RESET
        )
        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
            hideNavigation(true)
        }

        dialog.onNoClick = {
            dismissDialog()
        }

        dialog.onYesClick = {
            viewModel.checkDataInternet(this@AddCharacterActivity) {
                dismissDialog()
                lifecycleScope.launch {
                    showLoading()
                    withContext(Dispatchers.IO) {
                        viewModel.loadDataDefault(this@AddCharacterActivity)
                        viewModel.resetDraw()
                    }
                    binding.drawView.removeAllDraw()
                    binding.imvBackground.setImageBitmap(null)
                    binding.imvBackground.setBackgroundColor(getColor(R.color.transparent))
                    binding.functionPanels.textPanel.edtText.setText("")
                    binding.functionPanels.textPanel.edtText.setFont(viewModel.textFontList.first().color)
                    binding.functionPanels.textPanel.edtText.setTextColor(viewModel.textColorList[1].color)
                    binding.tvGetText.setFont(viewModel.textFontList.first().color)
                    binding.tvGetText.setTextColor(viewModel.textColorList[1].color)
                    addDrawable(viewModel.pathDefault, true)
                    backgroundImageAdapter.submitList(viewModel.backgroundImageList.map { it.copy() })
                    backgroundColorAdapter.submitList(viewModel.backgroundColorList.map { it.copy() })
                    stickerAdapter.submitList(viewModel.stickerList)
                    speechAdapter.submitList(viewModel.speechList)
                    textFontAdapter.submitListReset(viewModel.textFontList)
                    textColorAdapter.submitListReset(viewModel.textColorList)
                    dismissLoading(true)
                    // showInterAll()
                }
            }
        }
    }

    private fun handleSetBackgroundImage(path: String, position: Int) {
        binding.imvBackground.setBackgroundColor(getColor(R.color.transparent))
        loadImage(this, path, binding.imvBackground)
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.updateBackgroundImageSelected(position)
            withContext(Dispatchers.Main) {
                backgroundImageAdapter.submitItem(position, viewModel.backgroundImageList)
                backgroundColorAdapter.submitList(viewModel.backgroundColorList.map { it.copy() })
            }
        }
    }

    private fun checkStoragePermission() {
        // Use Photo Picker - NO PERMISSION REQUIRED
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun handleChooseColor(isTextColor: Boolean = false) {
        val dialog = ChooseColorDialog(this)

        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
            hideNavigation(true)
        }

        dialog.onCloseEvent = {
            dismissDialog()
        }

        dialog.onDoneEvent = { color ->
            dismissDialog()
            Log.d(
                "AddCharacterActivity",
                "Color picker selected color: ${
                    String.format(
                        "#%06X",
                        0xFFFFFF and color
                    )
                }, isTextColor=$isTextColor"
            )
            if (!isTextColor) {
                Log.d("AddCharacterActivity", "Calling handleSetBackgroundColor with position 0")
                handleSetBackgroundColor(color, 0)
            } else {
                Log.d("AddCharacterActivity", "Calling handleTextColorClick with position 0")
                handleTextColorClick(color, 0)
            }
        }
    }

    private fun handleSpeech(path: String) {
        val dialog = DialogSpeech(this, path)
        dialog.show()
        dialog.onDoneClick = { bitmap ->
            dialog.dismiss()
            hideNavigation(true)
            if (bitmap != null) {
                addDrawable("", false, bitmap)
            }
        }
    }

    private fun handleSetBackgroundColor(color: Int, position: Int) {
        Log.d(
            "AddCharacterActivity",
            "handleSetBackgroundColor called: color=${
                String.format(
                    "#%06X",
                    0xFFFFFF and color
                )
            }, position=$position"
        )
        binding.apply {
            imvBackground.setImageBitmap(null)
            imvBackground.setBackgroundColor(color)
            lifecycleScope.launch(Dispatchers.IO) {
                Log.d(
                    "AddCharacterActivity",
                    "Before updateBackgroundColorSelected: list[0].color=${
                        String.format(
                            "#%06X",
                            0xFFFFFF and viewModel.backgroundColorList[0].color
                        )
                    }"
                )
                viewModel.updateBackgroundColorSelected(position)
                Log.d(
                    "AddCharacterActivity",
                    "After updateBackgroundColorSelected: list[0].color=${
                        String.format(
                            "#%06X",
                            0xFFFFFF and viewModel.backgroundColorList[0].color
                        )
                    }, list[0].isSelected=${viewModel.backgroundColorList[0].isSelected}"
                )
                withContext(Dispatchers.Main) {
                    backgroundColorAdapter.submitItem(position, viewModel.backgroundColorList)
                    backgroundImageAdapter.submitList(viewModel.backgroundImageList.map { it.copy() })
                }
            }
        }
    }

    private fun handleFontClick(font: Int, position: Int) {
        binding.apply {
            functionPanels.textPanel.edtText.hint = SpannableString(getString(R.string.hello_world))
            functionPanels.textPanel.edtText.setFont(font)
            tvGetText.setFont(font)
            viewModel.updateTextFontSelected(position)
            textFontAdapter.submitItem(position, viewModel.textFontList)
        }
    }

    private fun handleTextColorClick(color: Int, position: Int) {
        Log.d(
            "AddCharacterActivity",
            "handleTextColorClick called: color=${
                String.format(
                    "#%06X",
                    0xFFFFFF and color
                )
            }, position=$position"
        )
        binding.apply {
            functionPanels.textPanel.edtText.setTextColor(color)
            tvGetText.setTextColor(color)
            Log.d(
                "AddCharacterActivity",
                "Before updateTextColorSelected: list[0].color=${
                    String.format(
                        "#%06X",
                        0xFFFFFF and viewModel.textColorList[0].color
                    )
                }"
            )
            viewModel.updateTextColorSelected(position)
            Log.d(
                "AddCharacterActivity",
                "After updateTextColorSelected: list[0].color=${
                    String.format(
                        "#%06X",
                        0xFFFFFF and viewModel.textColorList[0].color
                    )
                }, list[0].isSelected=${viewModel.textColorList[0].isSelected}"
            )
            textColorAdapter.submitItem(position, viewModel.textColorList)
        }
    }

    private fun handleDoneText() {
        viewModel.setIsFocusEditText(false)
        binding.apply {
            val text = functionPanels.textPanel.edtText.text.toString().trim()
            if (text.isEmpty()) {
                showToast(getString(R.string.null_edt))
                return
            }

            tvGetText.text = text
            val bitmap = BitmapHelper.getBitmapFromEditText(tvGetText)
            val drawableEmoji =
                viewModel.loadDrawableEmoji(this@AddCharacterActivity, bitmap, isText = true)
            drawView.addDraw(drawableEmoji)

            val font = viewModel.textFontList.first().color
            val color = viewModel.textColorList[1].color

            functionPanels.textPanel.edtText.text = null
            functionPanels.textPanel.edtText.setFont(font)
            functionPanels.textPanel.edtText.setTextColor(color)

            viewModel.updateTextFontSelected(0)
            viewModel.updateTextColorSelected(1)

            textFontAdapter.submitListReset(viewModel.textFontList)
            textColorAdapter.submitListReset(viewModel.textColorList)

            tvGetText.text = ""
            tvGetText.setFont(font)
            tvGetText.setTextColor(color)
        }
    }

    private fun clearFocus() {
        binding.drawView.hideSelect()
    }

    private fun handleSave() {
        binding.apply {
            clearFocus()
            lifecycleScope.launch(Dispatchers.IO) {
                showLoading()
                delay(200)
                viewModel.saveImageFromView(this@AddCharacterActivity, flSave).collect { result ->
                    when (result) {
                        is SaveState.Loading -> showLoading()

                        is SaveState.Error -> {
                            dismissLoading(true)
                            withContext(Dispatchers.Main) {
                                showToast(R.string.save_failed_please_try_again)
                            }
                        }

                        is SaveState.Success -> {
                            writeDesignSourceMetadata(result.path, "add_character")
                            val intent =
                                Intent(this@AddCharacterActivity, SuccessActivity::class.java)
                            intent.putExtra(IntentKey.INTENT_KEY, result.path)
                            intent.putExtra(IntentKey.STATUS_KEY, ValueKey.MY_DESIGN_TYPE)
                            val options = ActivityOptions.makeCustomAnimation(
                                this@AddCharacterActivity,
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                            dismissLoading(true)
                            withContext(Dispatchers.Main) {
                                showInterAll { startActivity(intent, options.toBundle()) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun writeDesignSourceMetadata(imagePath: String, source: String) {
        runCatching {
            val imageFile = File(imagePath)
            val metadataFile = File(imageFile.parent, "${imageFile.nameWithoutExtension}.json")
            metadataFile.writeText(
                buildString {
                    append("{\n")
                    append("  \"source\": \"$source\"\n")
                    append("}")
                }
            )
        }
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(
            this,
            getString(R.string.native_collap_bg),
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

    // Custom Input View Functions
    private fun setupCustomInput() {
        // val customInputView = binding.customInputLayout.root

        // Set height to 1/4 of screen
//        customInputView.post {
//            val screenHeight = resources.displayMetrics.heightPixels
//            val params = customInputView.layoutParams
//            params.height = screenHeight / 4
//            customInputView.layoutParams = params
//        }

        // Disable system keyboard for functionPanels.textPanel.edtText
        binding.functionPanels.textPanel.edtText.showSoftInputOnFocus = true

//        // Setup functionPanels.textPanel.edtText click listener
//        binding.functionPanels.textPanel.edtText.setOnClickListener {
//           // hideSystemKeyboard()
//          //  showCustomInput()
//        }

//        binding.functionPanels.textPanel.edtText.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus) {
//               // hideSystemKeyboard()
//                //showCustomInput()
//            }
//        }

        // Setup all letter buttons
        setupLetterButtons()

//        // Setup control buttons
//        binding.customInputLayout.btnSpace.tap { appendText(" ") }
//        binding.customInputLayout.btnDelete.tap { deleteLastChar() }
//        binding.customInputLayout.btnDone.tap { hideCustomInput() }
    }

    private fun setupLetterButtons() {
        val letters = listOf(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
        )

//        letters.forEach { letter ->
//            val buttonId = resources.getIdentifier("btn$letter", "id", packageName)
//            val button = binding.customInputLayout.root.findViewById<android.widget.Button>(buttonId)
//            button?.tap { appendText(letter) }
//        }
    }

//    private fun showCustomInput() {
//        binding.customInputLayout.root.visible()
//    }
//
//    private fun hideCustomInput() {
//        binding.customInputLayout.root.gone()
//    }

    private fun hideSystemKeyboard() {
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.functionPanels.textPanel.edtText.windowToken, 0)
    }

    private fun appendText(text: String) {
        val currentText = binding.functionPanels.textPanel.edtText.text.toString()
        binding.functionPanels.textPanel.edtText.setText(currentText + text)
        binding.functionPanels.textPanel.edtText.setSelection(binding.functionPanels.textPanel.edtText.text.length)
    }

    private fun deleteLastChar() {
        val currentText = binding.functionPanels.textPanel.edtText.text.toString()
        if (currentText.isNotEmpty()) {
            binding.functionPanels.textPanel.edtText.setText(currentText.dropLast(1))
            binding.functionPanels.textPanel.edtText.setSelection(binding.functionPanels.textPanel.edtText.text.length)
        }
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

package com.piratemaker.postermaker.poster.activity_app.add_character

import android.R.attr.bitmap
import android.R.attr.type
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.helper.AssetHelper
import com.piratemaker.postermaker.poster.core.helper.BitmapHelper
import com.piratemaker.postermaker.poster.core.helper.InternetHelper
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.core.helper.RemoteAssetHelper
import com.piratemaker.postermaker.poster.core.utils.DataLocal
import com.piratemaker.postermaker.poster.core.utils.key.AssetsKey
import com.piratemaker.postermaker.poster.core.utils.key.DomainKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.HandleState
import com.piratemaker.postermaker.poster.core.utils.state.SaveState
import com.piratemaker.postermaker.poster.data.model.SelectedModel
import com.piratemaker.postermaker.poster.core.draw.Draw
import com.piratemaker.postermaker.poster.core.draw.DrawableDraw
import com.piratemaker.postermaker.poster.dialog.DialogType
import com.piratemaker.postermaker.poster.dialog.YesNoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date

class AddCharacterViewModel : ViewModel() {
    private data class RemoteAssetConfig(
        val localAssetFolder: String,
        val remoteBaseUrl: String,
        val extensions: List<String>,
        val includePickerItem: Boolean = false
    )

    private val backgroundConfig = RemoteAssetConfig(
        localAssetFolder = AssetsKey.BACKGROUND_ASSET,
        remoteBaseUrl = DomainKey.getAddCharacterAssetUrl(AssetsKey.BACKGROUND_ASSET),
        extensions = listOf("png"),
        includePickerItem = true
    )

    private val stickerConfig = RemoteAssetConfig(
        localAssetFolder = AssetsKey.STICKER_ASSET,
        remoteBaseUrl = DomainKey.getAddCharacterAssetUrl(AssetsKey.STICKER_ASSET),
        extensions = listOf("png", "webp")
    )

    private val speechConfig = RemoteAssetConfig(
        localAssetFolder = AssetsKey.SPEECH_ASSET,
        remoteBaseUrl = DomainKey.getAddCharacterAssetUrl(AssetsKey.SPEECH_ASSET),
        extensions = listOf("png", "webp", "jpg")
    )

    var backgroundImageList: ArrayList<SelectedModel> = arrayListOf()
    var backgroundColorList: ArrayList<SelectedModel> = arrayListOf()
    var stickerList: ArrayList<SelectedModel> = arrayListOf()
    var speechList: ArrayList<SelectedModel> = arrayListOf()
    var textFontList: ArrayList<SelectedModel> = arrayListOf()
    var textColorList: ArrayList<SelectedModel> = arrayListOf()

    private val _typeNavigation = MutableStateFlow<Int>(-1)
    val typeNavigation = _typeNavigation.asStateFlow()

    private val _typeBackground = MutableStateFlow<Int>(-1)
    val typeBackground = _typeBackground.asStateFlow()

    private val _isFocusEditText = MutableStateFlow<Boolean>(false)
    val isFocusEditText = _isFocusEditText.asStateFlow()

    var currentDraw: Draw? = null

    var drawViewList: ArrayList<Draw> = arrayListOf()

    lateinit var layoutParams: ViewGroup.MarginLayoutParams

    var originalMarginBottom: Int = 0

    var pathDefault = ""

    fun setTypeNavigation(type: Int) {
        _typeNavigation.value = type
    }

    fun setTypeBackground(type: Int) {
        _typeBackground.value = type
    }

    fun setIsFocusEditText(status: Boolean) {
        _isFocusEditText.value = status
    }

    suspend fun loadDataDefault(context: Context) {
        backgroundImageList.clear()
        backgroundImageList.addAll(loadAssetOptions(context, backgroundConfig))

        backgroundColorList.clear()
        backgroundColorList.addAll(DataLocal.getBackgroundColorDefault(context))

        stickerList.clear()
        stickerList.addAll(loadAssetOptions(context, stickerConfig))

        speechList.clear()
        speechList.addAll(loadAssetOptions(context, speechConfig))

        textFontList.clear()
        textFontList.addAll(DataLocal.getTextFontDefault())
        textFontList.first().isSelected = true

        textColorList.clear()
        textColorList.addAll(DataLocal.getTextColorDefault(context))
        textColorList[1].isSelected = true
    }

    private suspend fun loadAssetOptions(context: Context, config: RemoteAssetConfig): ArrayList<SelectedModel> {
        val resolvedAssets = if (InternetHelper.checkInternet(context)) {
            val remoteAssets = RemoteAssetHelper.getSequentialRemoteAssets(config.remoteBaseUrl, config.extensions)
            if (remoteAssets.isNotEmpty()) {
                Log.d("AddCharacterViewModel", "Using remote ${config.localAssetFolder}: ${remoteAssets.size} items")
                remoteAssets
            } else {
                Log.d("AddCharacterViewModel", "Remote ${config.localAssetFolder} unavailable, falling back to local")
                AssetHelper.getSubfoldersAsset(context, config.localAssetFolder)
            }
        } else {
            Log.d("AddCharacterViewModel", "Offline, using local ${config.localAssetFolder}")
            AssetHelper.getSubfoldersAsset(context, config.localAssetFolder)
        }

        val items = resolvedAssets.map { SelectedModel(path = it) }.toCollection(ArrayList())
        if (config.includePickerItem) {
            items.add(0, SelectedModel())
        }
        return items
    }

    suspend fun updateBackgroundImageSelected(position: Int) {
        backgroundColorList = backgroundColorList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        backgroundImageList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    suspend fun updateBackgroundColorSelected(position: Int) {
        Log.d("AddCharacterViewModel", "updateBackgroundColorSelected called with position=$position")
        Log.d("AddCharacterViewModel", "Before update: backgroundColorList[0].color=${String.format("#%06X", 0xFFFFFF and backgroundColorList[0].color)}, isSelected=${backgroundColorList[0].isSelected}")

        backgroundImageList = backgroundImageList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        backgroundColorList.forEachIndexed { index, model ->
            Log.d("AddCharacterViewModel", "Setting position $index isSelected = ${index == position}")
            model.isSelected = index == position
        }

        Log.d("AddCharacterViewModel", "After update: backgroundColorList[0].color=${String.format("#%06X", 0xFFFFFF and backgroundColorList[0].color)}, isSelected=${backgroundColorList[0].isSelected}")
    }

    fun updateTextFontSelected(position: Int) {
        textFontList = textFontList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        textFontList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    fun updateTextColorSelected(position: Int) {
        Log.d("AddCharacterViewModel", "updateTextColorSelected called with position=$position")
        Log.d("AddCharacterViewModel", "Before update: textColorList[0].color=${String.format("#%06X", 0xFFFFFF and textColorList[0].color)}, isSelected=${textColorList[0].isSelected}")

        textColorList = textColorList.map { it.copy(isSelected = false) }.toCollection(ArrayList())
        textColorList.forEachIndexed { index, model ->
            Log.d("AddCharacterViewModel", "Setting position $index isSelected = ${index == position}")
            model.isSelected = index == position
        }

        Log.d("AddCharacterViewModel", "After update: textColorList[0].color=${String.format("#%06X", 0xFFFFFF and textColorList[0].color)}, isSelected=${textColorList[0].isSelected}")
    }

    fun checkDataInternet(context: AddCharacterActivity, action:
    (() -> Unit)) {
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                val dialog = YesNoDialog(
                    context,
                    R.string.no_internet,
                    R.string.please_check_your_internet,
                    isError = true,
                    dialogType = DialogType.INTERNET
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }


    fun updateCurrentCurrentDraw(draw: Draw) {
        currentDraw = draw
    }

    fun addDrawView(draw: Draw) {
        drawViewList.add(draw)
    }

    fun deleteDrawView(draw: Draw) {
        drawViewList.removeIf { it == draw }
    }

    fun updatePathDefault(path: String){
        pathDefault = path
    }
    fun loadDrawableEmoji(context: Context, bitmap: Bitmap, isCharacter: Boolean = false, isText: Boolean = false): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        val drawableEmoji = DrawableDraw(drawable, "${SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())}.png")
        drawableEmoji.isCharacter = isCharacter
        drawableEmoji.isText = isText
        return drawableEmoji
    }

    fun resetDraw() {
        drawViewList.clear()

    }

    fun saveImageFromView(context: Context, view: View): Flow<SaveState> = flow {
        emit(SaveState.Loading)
        val bitmap = BitmapHelper.createBimapFromView(view)
        MediaHelper.saveBitmapToInternalStorage(context, "bounty_designs", bitmap).collect { state ->
            emit(state)
        }
    }.flowOn(Dispatchers.IO)
}

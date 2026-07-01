package com.piratemaker.postermaker.poster.core.utils

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.custom.layout.LayoutPresets
import com.piratemaker.postermaker.poster.data.model.IntroModel
import com.piratemaker.postermaker.poster.data.model.LanguageModel
import com.piratemaker.postermaker.poster.data.model.SelectedModel
import com.piratemaker.postermaker.poster.data.model.custom.CustomizeModel
import com.facebook.shimmer.Shimmer
import com.piratemaker.postermaker.poster.core.utils.key.NavigationLayerKey
import com.piratemaker.postermaker.poster.data.model.custom.NavigationModel

object DataLocal {
    val shimmer =
        Shimmer.AlphaHighlightBuilder().setDuration(1800).setBaseAlpha(0.7f).setHighlightAlpha(0.6f)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT).setAutoStart(true).build()

    var lastClickTime = 0L
    var currentDate = ""
    var isConnectInternet = MutableLiveData<Boolean>()
    var isFailBaseURL = false
    var isCallDataAlready = false

    fun getLanguageList(): ArrayList<LanguageModel> {
        return arrayListOf(
            LanguageModel("hi", "Hindi", R.drawable.ic_flag_hindi),
            LanguageModel("es", "Spanish", R.drawable.ic_flag_spanish),
            LanguageModel("fr", "French", R.drawable.ic_flag_french),
            LanguageModel("en", "English", R.drawable.ic_flag_english),
            LanguageModel("pt", "Portuguese", R.drawable.ic_flag_portugeese),
            LanguageModel("in", "Indonesian", R.drawable.ic_flag_indo),
            LanguageModel("de", "German", R.drawable.ic_flag_germani),
        )
    }

    val itemIntroList = listOf(
        IntroModel(R.drawable.img_intro_1, R.string.title_1),
        IntroModel(R.drawable.img_intro_2, R.string.title_2),
        IntroModel(R.drawable.img_intro_3, R.string.title_3)
    )

    val bottomNavigationNotSelect = arrayListOf(
        R.drawable.ic_background,
        R.drawable.ic_sticker,
        R.drawable.ic_speech,
        R.drawable.ic_text,
    )

    val bottomNavigationSelected = arrayListOf(
        R.drawable.ic_background_selected,
        R.drawable.ic_sticker_selected,
        R.drawable.ic_speech_selected,
        R.drawable.ic_text_selected,
    )

    fun getBackgroundColorDefault(context: Context): ArrayList<SelectedModel> {
        return makerColorPalette(context).map { SelectedModel(color = it) }.toCollection(ArrayList())
    }

    fun getTextColorDefault(context: Context): ArrayList<SelectedModel> {
        return makerColorPalette(context).map { SelectedModel(color = it) }.toCollection(ArrayList())
    }

    fun getTextFontDefault(): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(color = R.font.roboto_regular),
            SelectedModel(color = R.font.roboto_bold),
            SelectedModel(color = R.font.roboto_medium),
            SelectedModel(color = R.font.montserrat_medium),
            SelectedModel(color = R.font.montserrat_bold),
            SelectedModel(color = R.font.londrina_solid_regular),
            SelectedModel(color = R.font.script_elegant_01),
            SelectedModel(color = R.font.script_elegant_02),
            SelectedModel(color = R.font.script_handwriting_01),
            SelectedModel(color = R.font.script_casual),
            SelectedModel(color = R.font.brush_01),
            SelectedModel(color = R.font.display_horror_02),
            SelectedModel(color = R.font.display_horror_04),
            SelectedModel(color = R.font.display_halloween),
            SelectedModel(color = R.font.display_gothic_01),
            SelectedModel(color = R.font.display_horror_11),
            SelectedModel(color = R.font.display_creative_01),
            SelectedModel(color = R.font.display_rounded),
            SelectedModel(color = R.font.serif_02),
            SelectedModel(color = R.font.serif_signature),
            SelectedModel(color = R.font.display_cultural),
            SelectedModel(color = R.font.display_festive),
            SelectedModel(color = R.font.treamd),
            SelectedModel(color = R.font.ocen),
        )
    }

    private fun makerColorPalette(context: Context): List<Int> {
        return listOf(
            "#FFFFFF",
            "#000000",
            "#A1CCEF",
            "#F68300",
            "#BD1313",
            "#F091F5",
            "#FFD54F",
            "#4CAF50",
            "#2196F3",
            "#9C27B0",
            "#FF5722",
            "#795548",
        ).map { android.graphics.Color.parseColor(it) }
    }
}

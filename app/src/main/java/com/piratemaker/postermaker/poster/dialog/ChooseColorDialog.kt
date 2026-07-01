package com.piratemaker.postermaker.poster.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseDialog
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.databinding.DialogColorPickerBinding
import com.piratemaker.postermaker.poster.core.helper.UnitHelper


class ChooseColorDialog(context: Context) : BaseDialog<DialogColorPickerBinding>(context,maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_color_picker
    override val isCancelOnTouchOutside: Boolean =false
    override val isCancelableByBack: Boolean = false

    var onDoneEvent: ((Int) -> Unit) = {}
    var onCloseEvent: (() -> Unit) = {}
    var onDismissEvent: (() -> Unit) = {}
    private var color = Color.WHITE
    override fun initView() {
        binding.apply {
            colorPickerView.apply {
                hueSliderView = hueSlider

                // Apply rounded corners programmatically
                val radiusPx = UnitHelper.dpToPx(context, 8f)
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }
                clipToOutline = true
            }
            binding.apply {
                tvDone.isSelected = true
                tvClose.isSelected = true
            }
        }
    }

    override fun initAction() {
        binding.apply {
            colorPickerView.setOnColorChangedListener {
                color = it
                // Update the color string display in real-time
             tvColorString.text = String.format("#%06X", 0xFFFFFF and it)
            }
            btnClose.tap { onCloseEvent.invoke() }
            btnDone.tap { onDoneEvent.invoke(color) }
        }
    }

    override fun onDismissListener() {
        onDismissEvent.invoke()
    }

}
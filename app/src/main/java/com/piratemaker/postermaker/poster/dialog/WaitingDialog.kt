package com.piratemaker.postermaker.poster.dialog

import android.app.Activity
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseDialog
import com.piratemaker.postermaker.poster.core.extensions.setBackgroundConnerSmooth
import com.piratemaker.postermaker.poster.databinding.DialogLoadingBinding

class WaitingDialog(val context: Activity) :
    BaseDialog<DialogLoadingBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_loading
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    override fun initView() {
    }

    override fun initAction() {}

    override fun onDismissListener() {}

}
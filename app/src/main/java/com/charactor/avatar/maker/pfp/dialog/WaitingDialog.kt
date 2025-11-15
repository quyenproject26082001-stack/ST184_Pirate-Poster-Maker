package com.charactor.avatar.maker.pfp.dialog

import android.app.Activity
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseDialog
import com.charactor.avatar.maker.pfp.core.extensions.setBackgroundConnerSmooth
import com.charactor.avatar.maker.pfp.databinding.DialogLoadingBinding

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
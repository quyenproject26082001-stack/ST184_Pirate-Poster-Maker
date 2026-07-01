package com.piratemaker.postermaker.poster.core.listener.listenerdraw

import android.view.MotionEvent
import com.piratemaker.postermaker.poster.core.custom.drawview.DrawView


class EditEvent : DrawEvent {
    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (!tattooView!!.isLocking()) {
            tattooView.editText()
        }
    }
}

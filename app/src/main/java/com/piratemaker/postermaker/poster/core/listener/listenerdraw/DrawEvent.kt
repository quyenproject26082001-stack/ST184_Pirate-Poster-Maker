package com.piratemaker.postermaker.poster.core.listener.listenerdraw

import android.view.MotionEvent
import com.piratemaker.postermaker.poster.core.custom.drawview.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}
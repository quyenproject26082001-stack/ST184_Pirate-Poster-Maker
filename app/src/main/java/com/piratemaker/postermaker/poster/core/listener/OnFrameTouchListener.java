package com.piratemaker.postermaker.poster.core.listener;

import android.view.MotionEvent;

public interface OnFrameTouchListener {
	public void onFrameTouch(MotionEvent event);
	public void onFrameDoubleClick(MotionEvent event);
}

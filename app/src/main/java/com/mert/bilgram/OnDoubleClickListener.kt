package com.mert.bilgram

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewConfiguration

abstract class OnDoubleClickListener internal constructor() : View.OnClickListener {
    private val doubleClickTimeout: Int = ViewConfiguration.getDoubleTapTimeout()
    private val handler: Handler
    private var firstClickTime: Long

    override fun onClick(v: View) {
        val now = System.currentTimeMillis()

        if (now - firstClickTime < doubleClickTimeout) {
            handler.removeCallbacksAndMessages(null)
            firstClickTime = 0L
            onDoubleClick(v)
        } else {
            firstClickTime = now
            handler.postDelayed({
                onSingleClick(v)
                firstClickTime = 0L
            }, doubleClickTimeout.toLong())
        }
    }

    abstract fun onDoubleClick(v: View?)
    abstract fun onSingleClick(v: View?)

    init {
        firstClickTime = 0L
        handler = Handler(Looper.getMainLooper())
    }
}
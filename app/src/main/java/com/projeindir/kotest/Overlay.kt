package com.projeindir.kotest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Oyunun üstünde duran küçük sürüklenebilir panel. */
class Overlay(
    private val ctx: Context,
    private val onTest: () -> Unit,
    private val onRun: () -> Unit,
    private val onClose: () -> Unit
) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null
    private lateinit var status: TextView
    private lateinit var testBtn: Button
    private lateinit var runBtn: Button
    private lateinit var lp: WindowManager.LayoutParams

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        val d = ctx.resources.displayMetrics.density
        fun btn(t: String, action: () -> Unit) = Button(ctx).apply {
            text = t
            textSize = 11f
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            setPadding((10 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (4 * d).toInt())
            setOnClickListener { action() }
        }
        testBtn = btn("TEST", onTest)
        runBtn = btn("BAŞLAT", onRun)
        val close = btn("X", onClose)

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(testBtn); addView(runBtn); addView(close)
        }
        status = TextView(ctx).apply {
            setTextColor(Color.WHITE)
            textSize = 10f
            text = "Hazır\n(sürüklemek için bu yazıdan tut)"
            setPadding((6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (6 * d).toInt())
        }
        val r = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xB0000000.toInt())
            addView(row); addView(status)
        }

        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = (ctx.resources.displayMetrics.heightPixels * 0.22f).toInt()
        }

        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        status.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = sx + (e.rawX - tx).toInt()
                    lp.y = sy + (e.rawY - ty).toInt()
                    root?.let { wm.updateViewLayout(it, lp) }
                }
            }
            true
        }

        wm.addView(r, lp)
        root = r
    }

    fun setStatus(s: String) { if (root != null) status.text = s }

    fun setMode(mode: String) {
        if (root == null) return
        testBtn.text = if (mode == "test") "TEST ■" else "TEST"
        runBtn.text = if (mode == "run") "DURDUR" else "BAŞLAT"
    }

    fun remove() {
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
    }
}

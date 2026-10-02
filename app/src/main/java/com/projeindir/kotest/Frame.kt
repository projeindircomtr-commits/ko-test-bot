package com.projeindir.kotest

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.view.Display

/** Ekran görüntüsünün ham RGBA baytları. */
class Frame(
    val w: Int,
    val h: Int,
    private val rowStride: Int,
    private val pxStride: Int,
    private val d: ByteArray
) {
    private fun i(x: Int, y: Int) = y * rowStride + x * pxStride
    fun r(x: Int, y: Int) = d[i(x, y)].toInt() and 0xFF
    fun g(x: Int, y: Int) = d[i(x, y) + 1].toInt() and 0xFF
    fun b(x: Int, y: Int) = d[i(x, y) + 2].toInt() and 0xFF
    fun px(f: Float) = (f * w).toInt().coerceIn(0, w - 1)
    fun py(f: Float) = (f * h).toInt().coerceIn(0, h - 1)
}

@Suppress("DEPRECATION")
fun realScreenSize(ctx: Context): Point {
    val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val p = Point()
    dm.getDisplay(Display.DEFAULT_DISPLAY).getRealSize(p)
    return p
}

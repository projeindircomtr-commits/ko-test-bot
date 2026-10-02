package com.projeindir.kotest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Oyun ekranında hazır slotların üstüne numaralarını çizer (dokunmaları engellemez). */
class SlotLabels(ctx: Context) : View(ctx) {
    private val d = ctx.resources.displayMetrics.density
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * d; color = Color.YELLOW
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC000000.toInt() }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW; textSize = 15 * d; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val rad = 13 * d
        KoMobile.SLOT_POINTS.forEachIndexed { i, p ->
            val x = p.x * width
            val y = p.y * height
            c.drawCircle(x, y, rad, fill)
            c.drawCircle(x, y, rad, ring)
            c.drawText("${i + 1}", x, y + txt.textSize / 3, txt)
        }
    }
}

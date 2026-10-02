package com.projeindir.kotest

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import java.util.Random

class TapService : AccessibilityService() {
    companion object {
        @Volatile var instance: TapService? = null
    }

    private val rnd = Random()

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    private fun tap(x: Float, y: Float, durMs: Long) {
        val path = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durMs))
            .build()
        dispatchGesture(g, null, null)
    }

    /** Parmak gibi: nokta her seferinde biraz kayar, basma süresi değişir. */
    fun tapNatural(p: P) {
        val s = realScreenSize(this)
        val j = s.x * KoMobile.TAP_JITTER
        val dx = (rnd.nextGaussian() * j / 2).toFloat().coerceIn(-j, j)
        val dy = (rnd.nextGaussian() * j / 2).toFloat().coerceIn(-j, j)
        val x = (p.x * s.x + dx).coerceIn(1f, s.x - 1f)
        val y = (p.y * s.y + dy).coerceIn(1f, s.y - 1f)
        tap(x, y, 70L + rnd.nextInt(90))
    }
}

package com.projeindir.kotest

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

/** Erişilebilirlik servisi: sadece ekrana dokunma işi yapar. */
class TapService : AccessibilityService() {
    companion object {
        @Volatile var instance: TapService? = null
    }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        dispatchGesture(g, null, null)
    }

    fun tapRatio(p: P) {
        val s = realScreenSize(this)
        tap(p.x * s.x, p.y * s.y)
    }
}

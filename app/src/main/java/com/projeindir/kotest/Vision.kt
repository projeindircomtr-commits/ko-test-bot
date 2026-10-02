package com.projeindir.kotest

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class BarRead(val fill: Float, val startsLeft: Boolean, val hits: Int)

data class State(
    val hp: Float,
    val mp: Float,
    val target: Boolean,
    val targetHp: Float,
    val lootScore: Int
) {
    val loot get() = lootScore >= KoMobile.LOOT_MIN
    private fun pct(v: Float) = (v * 100).roundToInt()
    fun text() = "HP %${pct(hp)}  MP %${pct(mp)}\n" +
        "Hedef: " + (if (target) "VAR %${pct(targetHp)}" else "YOK") + "\n" +
        "Sandık: " + (if (loot) "VAR" else "YOK") + " ($lootScore/${Vision.LOOT_SAMPLES})"
}

object Vision {
    const val LOOT_SAMPLES = 24

    private fun red(r: Int, g: Int, b: Int) = r > 120 && r > g * 2 && r > b * 2
    private fun blue(r: Int, g: Int, b: Int) = b > 110 && b * 2 > r * 3 && b * 5 > g * 6
    private fun gold(r: Int, g: Int, b: Int) = r > 170 && g > 100 && b < 80 && r >= g && r - b > 110

    /** Barın doluluğunu en sağdaki renkli pikselden hesaplar (üstteki beyaz yazı etkilemez). */
    fun bar(f: Frame, a: Bar, test: (Int, Int, Int) -> Boolean): BarRead {
        val x1 = f.px(a.x1); val x2 = f.px(a.x2)
        val y1 = f.py(a.y1); val y2 = f.py(a.y2)
        var first = -1; var last = -1; var hits = 0
        for (k in 1..3) {
            val y = y1 + (y2 - y1) * k / 4
            for (x in x1..x2) {
                if (test(f.r(x, y), f.g(x, y), f.b(x, y))) {
                    hits++
                    if (first < 0 || x < first) first = x
                    if (x > last) last = x
                }
            }
        }
        val span = (x2 - x1).coerceAtLeast(1)
        val fill = if (last < 0) 0f else ((last - x1).toFloat() / span).coerceIn(0f, 1f)
        val startsLeft = first >= 0 && (first - x1) <= span * 6 / 100
        return BarRead(fill, startsLeft, hits)
    }

    /** Sandık butonunun altın halkasında kaç nokta altın renkte. */
    fun loot(f: Frame): Int {
        val cx = KoMobile.LOOT_BTN.x * f.w
        val cy = KoMobile.LOOT_BTN.y * f.h
        var score = 0
        for (i in 0 until LOOT_SAMPLES) {
            val ang = 2.0 * Math.PI * i / LOOT_SAMPLES
            for (rr in KoMobile.LOOT_RADII) {
                val rp = rr * f.w
                val x = (cx + rp * cos(ang)).toInt().coerceIn(0, f.w - 1)
                val y = (cy + rp * sin(ang)).toInt().coerceIn(0, f.h - 1)
                if (gold(f.r(x, y), f.g(x, y), f.b(x, y))) { score++; break }
            }
        }
        return score
    }

    fun read(f: Frame): State {
        val hp = bar(f, KoMobile.HP_BAR) { r, g, b -> red(r, g, b) }
        val mp = bar(f, KoMobile.MP_BAR) { r, g, b -> blue(r, g, b) }
        val t = bar(f, KoMobile.TARGET_BAR) { r, g, b -> red(r, g, b) }
        val hasTarget = t.startsLeft && t.hits >= 3
        return State(hp.fill, mp.fill, hasTarget, if (hasTarget) t.fill else 0f, loot(f))
    }
}

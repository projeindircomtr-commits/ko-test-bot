package com.projeindir.kotest

data class P(val x: Float, val y: Float)
data class Bar(val x1: Float, val x2: Float, val y1: Float, val y2: Float)

/**
 * KO Mobile ekran profili. Referans 2576x1159 yatay ekran; tüm değerler ekran ORANI (0..1).
 * Skill/pot slotları burada değil, kullanıcı oyunda +SLOT ile kendisi ekler.
 */
object KoMobile {
    val HP_BAR = Bar(0.103f, 0.171f, 0.011f, 0.021f)
    val MP_BAR = Bar(0.103f, 0.171f, 0.036f, 0.045f)
    val TARGET_BAR = Bar(0.431f, 0.569f, 0.054f, 0.070f)

    val LOOT_BTN = P(0.2686f, 0.591f)
    // Buton tanıma: altın sandığın etrafındaki koyu daire + ortadaki altın sandık
    const val LOOT_DARK_R = 0.0225f   // koyu daire yarıçapı (genişliğe oran)
    const val LOOT_GOLD_R = 0.008f    // ortadaki sandık
    const val LOOT_DARK_MIN = 14      // 24 noktanın en az bu kadarı koyu
    const val LOOT_GOLD_MIN = 8       // 24 noktanın en az bu kadarı altın

    val SELECT_BTN = P(0.953f, 0.906f)  // Z
    val ATTACK_BTN = P(0.936f, 0.781f)

    // Skill halkasındaki hazır slotlar (No 1..17). Referans piksel / 2576x1159
    private fun r(x: Int, y: Int) = P(x / 2576f, y / 1159f)
    val SLOT_POINTS = listOf(
        r(2075, 415), r(2337, 485), r(2474, 485), r(2065, 555), r(2232, 598),
        r(1972, 697), r(2130, 697), r(2318, 720), r(2455, 715), r(1798, 780),
        r(1933, 845), r(2080, 838), r(2228, 828), r(1823, 950), r(1963, 992),
        r(2098, 978), r(2238, 960)
    )

    // Yönetici şartı: seri/hızlı basma yok. İki dokunuş arası en az bu kadar + rastgele ek.
    const val MIN_GAP_MS = 700L
    const val GAP_SPREAD_MS = 600
    const val TAP_JITTER = 0.006f       // dokunma noktası sapması (genişliğe oran)
    const val STUCK_MS = 12000L
    const val LOOT_WAIT_MS = 800L
}

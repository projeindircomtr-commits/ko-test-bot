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
    val LOOT_RADII = floatArrayOf(0.0225f, 0.0240f, 0.0252f)
    const val LOOT_MIN = 14

    val SELECT_BTN = P(0.953f, 0.906f)  // Z
    val ATTACK_BTN = P(0.936f, 0.781f)

    // Yönetici şartı: seri/hızlı basma yok. İki dokunuş arası en az bu kadar + rastgele ek.
    const val MIN_GAP_MS = 700L
    const val GAP_SPREAD_MS = 600
    const val TAP_JITTER = 0.006f       // dokunma noktası sapması (genişliğe oran)
    const val STUCK_MS = 12000L
    const val LOOT_WAIT_MS = 800L
}

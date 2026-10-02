package com.projeindir.kotest

data class P(val x: Float, val y: Float)
data class Bar(val x1: Float, val x2: Float, val y1: Float, val y2: Float)

/**
 * KO Mobile ekran profili.
 * Referans: 2576x1159 yatay ekran görüntüsü. Tüm değerler ekran ORANI (0..1),
 * böylece farklı çözünürlükte telefonlarda da aynı yere denk gelir.
 */
object KoMobile {
    // Sol üst barlar
    val HP_BAR = Bar(0.103f, 0.171f, 0.011f, 0.021f)
    val MP_BAR = Bar(0.103f, 0.171f, 0.036f, 0.045f)

    // Hedef seçilince üst ortada çıkan can barı (örn. "Bulcan 99/99")
    val TARGET_BAR = Bar(0.431f, 0.569f, 0.054f, 0.070f)

    // Sandık toplama butonu (ortadaki büyük altın sandık)
    val LOOT_BTN = P(0.2686f, 0.591f)
    val LOOT_RADII = floatArrayOf(0.0225f, 0.0240f, 0.0252f) // halka yarıçapı (genişliğe oran)
    const val LOOT_MIN = 14 // 24 noktanın kaçı altın renkse "buton var" sayılır

    // Butonlar
    val SELECT_BTN = P(0.953f, 0.906f)  // Z - hedef seç
    val ATTACK_BTN = P(0.936f, 0.781f)  // büyük saldırı butonu

    // Sırayla basılacak skiller (sağdaki halka)
    val SKILLS = listOf(
        P(0.8075f, 0.723f),  // kılıç/slash ikonu
        P(0.8269f, 0.6014f), // kırmızı spiral
        P(0.8665f, 0.516f)   // kırmızı girdap
    )

    const val SKILL_DELAY_MS = 1300L   // iki skill arası bekleme
    const val MIN_MP = 0.10f           // MP bunun altındaysa skill basma
    const val STUCK_MS = 12000L        // hedefin canı bu kadar süre azalmazsa hedef değiştir
    const val SELECT_EVERY_MS = 900L   // hedef yokken Z'ye basma aralığı
    const val LOOT_WAIT_MS = 700L      // sandığa bastıktan sonra bekleme
}

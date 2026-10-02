package com.projeindir.kotest

import java.util.Random
import kotlin.math.abs

/**
 * Öncelik: HP pot > MP pot > sandık > hedef seç > saldır > hazır olan ilk skill.
 * Her dokunuş arasında en az MIN_GAP_MS + rastgele bekleme var.
 * sessionMin çalıştıktan sonra breakMin dakika zorunlu mola verir.
 */
class BotEngine(
    private val cap: ScreenCaptureService,
    private val dryRun: Boolean,
    private val slots: List<Slot>,
    private val sessionMin: Int,
    private val breakMin: Int,
    private val status: (String) -> Unit
) {
    @Volatile private var running = false
    private var worker: Thread? = null
    private val rnd = Random()

    fun start() {
        running = true
        worker = Thread({ loop() }, "bot").also { it.start() }
    }

    fun stop() {
        running = false
        worker?.interrupt()
    }

    private fun loop() {
        val lastUse = LongArray(slots.size)
        val extra = LongArray(slots.size)
        var lastTap = 0L
        var nextGap = KoMobile.MIN_GAP_MS
        var engaged = false
        var lastHp = -1f
        var hpChangedAt = 0L
        var lastStatus = 0L
        var sessionStart = System.currentTimeMillis()
        val skillCount = slots.count { it.type == SlotType.SKILL }
        val potCount = slots.size - skillCount

        try {
            while (running) {
                var now = System.currentTimeMillis()

                // Zorunlu mola
                if (!dryRun && now - sessionStart > sessionMin * 60_000L) {
                    val until = now + breakMin * 60_000L
                    while (running && System.currentTimeMillis() < until) {
                        val left = (until - System.currentTimeMillis()) / 1000
                        status("[MOLA]\nKalan: ${left / 60} dk ${left % 60} sn")
                        Thread.sleep(1000)
                    }
                    sessionStart = System.currentTimeMillis()
                    engaged = false
                    continue
                }

                val f = cap.grab()
                val t = TapService.instance
                if (f == null) { status("Görüntü bekleniyor..."); Thread.sleep(400); continue }
                val s = Vision.read(f)
                now = System.currentTimeMillis()

                if (now - lastStatus > 400) {
                    val head = if (dryRun) "[TEST - basmıyor]" else {
                        val left = (sessionMin * 60_000L - (now - sessionStart)) / 60_000
                        "[ÇALIŞIYOR] molaya ~$left dk"
                    }
                    val warn = if (t == null) "\n! Erişilebilirlik kapalı" else ""
                    status("$head\n${s.text()}\nSlot: $skillCount skill, $potCount pot$warn")
                    lastStatus = now
                }
                if (dryRun || t == null) { Thread.sleep(300); continue }

                // Seri basma yok
                if (now - lastTap < nextGap) { Thread.sleep(80); continue }

                fun ready(i: Int) = now - lastUse[i] >= (slots[i].seconds * 1000).toLong() + extra[i]
                fun press(p: P, slot: Int = -1) {
                    t.tapNatural(p)
                    lastTap = System.currentTimeMillis()
                    nextGap = KoMobile.MIN_GAP_MS + rnd.nextInt(KoMobile.GAP_SPREAD_MS)
                    if (slot >= 0) { lastUse[slot] = lastTap; extra[slot] = rnd.nextInt(500).toLong() }
                }

                // 1) Potlar (yaklaşık okuma yeterli)
                val pot = slots.indices.firstOrNull { i ->
                    val sl = slots[i]
                    ready(i) && when (sl.type) {
                        SlotType.HP -> s.hpSeen && s.hp * 100 < sl.percent
                        SlotType.MP -> s.mpSeen && s.mp * 100 < sl.percent
                        SlotType.SKILL -> false
                    }
                }
                if (pot != null) { press(P(slots[pot].x, slots[pot].y), pot); continue }

                // 2) Sandık
                if (s.loot) { press(KoMobile.LOOT_BTN); Thread.sleep(KoMobile.LOOT_WAIT_MS); continue }

                // 3) Hedef yoksa seç
                if (!s.target) { engaged = false; press(KoMobile.SELECT_BTN); continue }

                // 4) Yeni hedefe saldırı başlat
                if (!engaged) {
                    engaged = true
                    lastHp = s.targetHp
                    hpChangedAt = now
                    press(KoMobile.ATTACK_BTN)
                    continue
                }

                // 5) Takılma: hedefin canı uzun süre değişmezse yeni hedef
                if (abs(s.targetHp - lastHp) > 0.01f) { lastHp = s.targetHp; hpChangedAt = now }
                if (now - hpChangedAt > KoMobile.STUCK_MS) {
                    engaged = false
                    hpChangedAt = now
                    press(KoMobile.SELECT_BTN)
                    continue
                }

                // 6) Süresi dolan ilk skill (liste sırasıyla)
                val sk = slots.indices.firstOrNull { slots[it].type == SlotType.SKILL && ready(it) }
                if (sk != null) press(P(slots[sk].x, slots[sk].y), sk) else Thread.sleep(120)
            }
        } catch (_: InterruptedException) {
        }
    }
}

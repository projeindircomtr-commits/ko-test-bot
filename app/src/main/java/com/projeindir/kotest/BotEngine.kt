package com.projeindir.kotest

import kotlin.math.abs

/**
 * Farm döngüsü: sandık varsa topla -> hedef yoksa Z -> hedef varsa saldır + skill sırası.
 * dryRun = true ise sadece ekranı okur, hiçbir yere basmaz (TEST modu).
 */
class BotEngine(
    private val cap: ScreenCaptureService,
    private val dryRun: Boolean,
    private val status: (String) -> Unit
) {
    @Volatile private var running = false
    private var worker: Thread? = null

    fun start() {
        running = true
        worker = Thread({ loop() }, "bot").also { it.start() }
    }

    fun stop() {
        running = false
        worker?.interrupt()
    }

    private fun loop() {
        var lastSkill = 0L
        var skillIdx = 0
        var engaged = false
        var lastHp = -1f
        var hpChangedAt = 0L
        var lastSelect = 0L
        var lastStatus = 0L
        try {
            while (running) {
                val f = cap.grab()
                val t = TapService.instance
                val now = System.currentTimeMillis()
                if (f == null) {
                    status("Görüntü bekleniyor...")
                    Thread.sleep(400)
                    continue
                }
                val s = Vision.read(f)
                if (now - lastStatus > 300) {
                    val head = if (dryRun) "[TEST - basmıyor]\n" else "[ÇALIŞIYOR]\n"
                    val warn = if (t == null) "\n! Erişilebilirlik kapalı" else ""
                    status(head + s.text() + warn)
                    lastStatus = now
                }
                if (dryRun || t == null) {
                    Thread.sleep(300)
                    continue
                }

                // 1) Sandık butonu görünüyorsa topla
                if (s.loot) {
                    t.tapRatio(KoMobile.LOOT_BTN)
                    Thread.sleep(KoMobile.LOOT_WAIT_MS)
                    continue
                }

                // 2) Hedef yoksa Z ile seç
                if (!s.target) {
                    engaged = false
                    if (now - lastSelect > KoMobile.SELECT_EVERY_MS) {
                        t.tapRatio(KoMobile.SELECT_BTN)
                        lastSelect = now
                    }
                    Thread.sleep(250)
                    continue
                }

                // 3) Yeni hedef: saldırıyı başlat
                if (!engaged) {
                    engaged = true
                    t.tapRatio(KoMobile.ATTACK_BTN)
                    lastHp = s.targetHp
                    hpChangedAt = now
                    Thread.sleep(250)
                    continue
                }

                // 4) Takılma kontrolü: can uzun süre değişmezse hedef değiştir
                if (abs(s.targetHp - lastHp) > 0.01f) {
                    lastHp = s.targetHp
                    hpChangedAt = now
                }
                if (now - hpChangedAt > KoMobile.STUCK_MS) {
                    t.tapRatio(KoMobile.SELECT_BTN)
                    engaged = false
                    hpChangedAt = now
                    Thread.sleep(400)
                    continue
                }

                // 5) Skill sırası
                if (KoMobile.SKILLS.isNotEmpty() && s.mp >= KoMobile.MIN_MP &&
                    now - lastSkill > KoMobile.SKILL_DELAY_MS
                ) {
                    t.tapRatio(KoMobile.SKILLS[skillIdx])
                    skillIdx = (skillIdx + 1) % KoMobile.SKILLS.size
                    lastSkill = now
                }
                Thread.sleep(200)
            }
        } catch (_: InterruptedException) {
        }
    }
}

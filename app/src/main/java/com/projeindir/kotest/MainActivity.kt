package com.projeindir.kotest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val reqCapture = 41
    private lateinit var info: TextView
    private lateinit var slotBox: LinearLayout
    private lateinit var sessionEt: EditText
    private lateinit var breakEt: EditText
    private var slots = mutableListOf<Slot>()

    private class Row(val sec: EditText, val pct: EditText)
    private val rows = mutableListOf<Row>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        fun label(t: String, size: Float = 14f) =
            TextView(this).apply { text = t; textSize = size; setPadding(0, pad / 2, 0, pad / 4) }.also { root.addView(it) }
        fun btn(t: String, action: () -> Unit) =
            Button(this).apply { text = t; setOnClickListener { action() } }.also { root.addView(it) }

        label("Projeindir Bot – KO Mobile", 20f)
        info = label("")
        btn("1) Erişilebilirlik iznini aç") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        btn("2) Üstte gösterme iznini aç") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btn("3) Ekran yakalamayı başlat") { startCapture() }
        btn("Durdur") { stopService(Intent(this, ScreenCaptureService::class.java)) }

        label("Çalışma / mola", 17f)
        label("Bot bu kadar dakika çalışır, sonra zorunlu mola verir.")
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        sessionEt = num("Çalışma dk", false)
        breakEt = num("Mola dk", false)
        timeRow.addView(TextView(this).apply { text = "Çalışma dk: " })
        timeRow.addView(sessionEt)
        timeRow.addView(TextView(this).apply { text = "  Mola dk: " })
        timeRow.addView(breakEt)
        root.addView(timeRow)

        label("Slotlar", 17f)
        label("Numarayla ata: oyunda paneldeki NO butonuna bas, slot numaralarını gör, buradan No yazıp Ata'ya bas. " +
            "Halkada olmayan bir yer için paneldeki +SLOT ile dokunarak da ekleyebilirsin. Tip butonuna basarak Skill / HP pot / MP pot arasında değiştir.\n" +
            "Skill: \"sn\" = kaç saniyede bir basılsın.\nPot: \"%\" = can/mana bunun altına düşünce bas, \"sn\" = potun bekleme süresi.")
        val addRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val noEt = num("No", false)
        addRow.addView(TextView(this).apply { text = "Slot No (1-${KoMobile.SLOT_POINTS.size}): " })
        addRow.addView(noEt)
        addRow.addView(Button(this).apply {
            text = "Ata"
            setOnClickListener {
                val n = noEt.text.toString().toIntOrNull()
                if (n == null || n !in 1..KoMobile.SLOT_POINTS.size) {
                    Toast.makeText(this@MainActivity, "1 ile ${KoMobile.SLOT_POINTS.size} arası yaz", Toast.LENGTH_SHORT).show()
                } else if (slots.any { it.no == n }) {
                    Toast.makeText(this@MainActivity, "No $n zaten ekli", Toast.LENGTH_SHORT).show()
                } else {
                    collect()
                    val p = KoMobile.SLOT_POINTS[n - 1]
                    slots.add(Slot(p.x, p.y, SlotType.SKILL, 1.5f, 50, n))
                    Store.saveSlots(this@MainActivity, slots)
                    noEt.setText("")
                    render()
                }
            }
        })
        root.addView(addRow)
        slotBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(slotBox)
        btn("Kaydet") { saveAll(); Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show() }

        label("Kullanım: oyunda önce TEST ile okunan değerlere bak, sonra BAŞLAT. " +
            "Ayarı değiştirdikten sonra paneldeki BAŞLAT'a yeniden bas.")
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun num(hint: String, decimal: Boolean) = EditText(this).apply {
        this.hint = hint
        inputType = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        else InputType.TYPE_CLASS_NUMBER
        minEms = 3
    }

    override fun onResume() {
        super.onResume()
        val acc = if (TapService.instance != null) "AÇIK" else "KAPALI"
        val ovl = if (Settings.canDrawOverlays(this)) "AÇIK" else "KAPALI"
        info.text = "Erişilebilirlik: $acc\nÜstte gösterme: $ovl"
        sessionEt.setText(Store.sessionMin(this).toString())
        breakEt.setText(Store.breakMin(this).toString())
        slots = Store.loadSlots(this)
        render()
    }

    private fun render() {
        slotBox.removeAllViews()
        rows.clear()
        if (slots.isEmpty()) {
            slotBox.addView(TextView(this).apply { text = "Henüz slot yok." })
            return
        }
        slots.forEachIndexed { i, s ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(TextView(this).apply { text = if (s.no > 0) "No ${s.no} " else "Özel " })
            row.addView(Button(this).apply {
                text = s.type.label
                setOnClickListener {
                    collect()
                    s.type = SlotType.values()[(s.type.ordinal + 1) % SlotType.values().size]
                    if (s.type != SlotType.SKILL && s.seconds < 2f) s.seconds = 2f
                    render()
                }
            })
            val sec = num("sn", true).apply { setText(trim(s.seconds)) }
            val pct = num("%", false).apply {
                setText(s.percent.toString())
                isEnabled = s.type != SlotType.SKILL
            }
            row.addView(TextView(this).apply { text = " sn:" }); row.addView(sec)
            row.addView(TextView(this).apply { text = " %:" }); row.addView(pct)
            row.addView(Button(this).apply {
                text = "Sil"
                setOnClickListener { collect(); slots.removeAt(i); Store.saveSlots(this@MainActivity, slots); render() }
            })
            rows.add(Row(sec, pct))
            slotBox.addView(row)
        }
    }

    private fun trim(f: Float) = if (f == f.toInt().toFloat()) f.toInt().toString() else f.toString()

    private fun collect() {
        if (rows.size != slots.size) return
        rows.forEachIndexed { i, r ->
            r.sec.text.toString().replace(',', '.').toFloatOrNull()?.let { slots[i].seconds = it.coerceIn(0.5f, 600f) }
            r.pct.text.toString().toIntOrNull()?.let { slots[i].percent = it.coerceIn(5, 95) }
        }
    }

    private fun saveAll() {
        collect()
        Store.saveSlots(this, slots)
        val sm = sessionEt.text.toString().toIntOrNull() ?: Store.sessionMin(this)
        val bm = breakEt.text.toString().toIntOrNull() ?: Store.breakMin(this)
        Store.saveTimes(this, sm, bm)
        sessionEt.setText(Store.sessionMin(this).toString())
        breakEt.setText(Store.breakMin(this).toString())
        render()
    }

    override fun onPause() {
        saveAll()
        super.onPause()
    }

    private fun startCapture() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Önce 2. izni aç", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 5)
        }
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(mpm.createScreenCaptureIntent(), reqCapture)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != reqCapture) return
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Ekran izni verilmedi", Toast.LENGTH_SHORT).show()
            return
        }
        val i = Intent(this, ScreenCaptureService::class.java)
            .putExtra(ScreenCaptureService.EXTRA_CODE, resultCode)
            .putExtra(ScreenCaptureService.EXTRA_DATA, data)
        startForegroundService(i)
        Toast.makeText(this, "Hazır. KO Mobile'ı aç, +SLOT ile slotlarını ekle.", Toast.LENGTH_LONG).show()
    }
}

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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val reqCapture = 41
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply { text = "Projeindir Bot – KO Mobile Test"; textSize = 20f })
        info = TextView(this).apply { textSize = 14f; setPadding(0, pad / 2, 0, pad / 2) }
        root.addView(info)

        fun btn(t: String, action: () -> Unit) {
            root.addView(Button(this).apply { text = t; setOnClickListener { action() } })
        }
        btn("1) Erişilebilirlik iznini aç") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("2) Üstte gösterme iznini aç") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btn("3) Ekran yakalamayı başlat") { startCapture() }
        btn("Durdur") { stopService(Intent(this, ScreenCaptureService::class.java)) }

        root.addView(TextView(this).apply {
            textSize = 13f
            setPadding(0, pad, 0, 0)
            text = "Kullanım:\n• 3. adımda \"Tüm ekran\" seç.\n• KO Mobile'ı aç, soldaki panelde önce TEST'e bas: " +
                "bot sadece okur, hiçbir yere basmaz.\n• HP/MP/Hedef/Sandık değerleri doğruysa BAŞLAT.\n" +
                "• Paneli hedef barının veya sandık butonunun üstüne sürükleme."
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val acc = if (TapService.instance != null) "AÇIK" else "KAPALI"
        val ovl = if (Settings.canDrawOverlays(this)) "AÇIK" else "KAPALI"
        info.text = "Erişilebilirlik: $acc\nÜstte gösterme: $ovl"
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
        Toast.makeText(this, "Hazır. KO Mobile'ı aç, paneldeki TEST ile dene.", Toast.LENGTH_LONG).show()
    }
}

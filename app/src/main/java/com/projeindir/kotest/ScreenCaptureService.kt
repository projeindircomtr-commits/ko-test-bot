package com.projeindir.kotest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

class ScreenCaptureService : Service() {
    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        private const val CH = "bot"
        private const val NID = 7
    }

    private var projection: MediaProjection? = null
    private var vDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var overlay: Overlay? = null
    private var engine: BotEngine? = null
    private var mode = "idle"
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var bytes: ByteArray? = null
    private var lastFrame: Frame? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        if (projection != null) return START_NOT_STICKY
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val data = intent?.let { dataOf(it) }
        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            setup(code, data)
        } catch (e: Exception) {
            Toast.makeText(this, "Başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun dataOf(i: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else i.getParcelableExtra(EXTRA_DATA)

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Bot", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, CH)
            .setContentTitle("Projeindir Bot")
            .setContentText("Ekran okunuyor")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NID, n)
        }
    }

    private fun setup(code: Int, data: Intent) {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mp = mpm.getMediaProjection(code, data)
        val ht = HandlerThread("capture").also { it.start() }
        thread = ht
        val h = Handler(ht.looper)
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { main.post { stopSelf() } }
        }, h)

        // Oyun yatay olduğu için her zaman yatay boyut, yarım çözünürlük (hız için)
        val sz = realScreenSize(this)
        val w = max(sz.x, sz.y) / 2
        val hh = min(sz.x, sz.y) / 2
        val rd = ImageReader.newInstance(w, hh, PixelFormat.RGBA_8888, 2)
        reader = rd
        vDisplay = mp.createVirtualDisplay(
            "ko-test", w, hh, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, rd.surface, null, h
        )
        projection = mp

        overlay = Overlay(this, { toggle("test") }, { toggle("run") }, { stopSelf() }).also { it.show() }
    }

    private fun toggle(m: String) {
        engine?.stop()
        engine = null
        if (mode == m) {
            mode = "idle"
            overlay?.setMode(mode)
            overlay?.setStatus("Durduruldu")
            return
        }
        mode = m
        engine = BotEngine(this, m == "test") { s -> main.post { overlay?.setStatus(s) } }
            .also { it.start() }
        overlay?.setMode(mode)
    }

    /** En son ekran karesini verir (yeni kare yoksa bir öncekini). */
    fun grab(): Frame? = synchronized(lock) {
        val rd = reader ?: return null
        val img = try { rd.acquireLatestImage() } catch (_: Exception) { null } ?: return lastFrame
        try {
            val pl = img.planes[0]
            val buf = pl.buffer
            buf.rewind()
            val n = buf.remaining()
            var arr = bytes
            if (arr == null || arr.size != n) { arr = ByteArray(n); bytes = arr }
            buf.get(arr, 0, n)
            lastFrame = Frame(img.width, img.height, pl.rowStride, pl.pixelStride, arr)
        } finally {
            img.close()
        }
        lastFrame
    }

    override fun onDestroy() {
        engine?.stop()
        engine = null
        overlay?.remove()
        overlay = null
        synchronized(lock) {
            vDisplay?.release(); vDisplay = null
            reader?.close(); reader = null
        }
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        thread?.quitSafely()
        super.onDestroy()
    }
}

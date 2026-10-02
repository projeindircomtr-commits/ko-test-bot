package com.projeindir.kotest

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class SlotType(val label: String) { SKILL("Skill"), HP("HP pot"), MP("MP pot") }

/** seconds: skill için bekleme süresi; percent: pot için "bunun altına düşünce bas". */
/** no: hazır slot numarası (1..17), 0 = ekrandan dokunarak eklenmiş özel konum. */
data class Slot(var x: Float, var y: Float, var type: SlotType, var seconds: Float, var percent: Int, var no: Int = 0)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("bot", Context.MODE_PRIVATE)

    fun loadSlots(c: Context): MutableList<Slot> {
        val raw = prefs(c).getString("slots", null) ?: return mutableListOf()
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                Slot(
                    o.getDouble("x").toFloat(), o.getDouble("y").toFloat(),
                    SlotType.valueOf(o.getString("t")),
                    o.getDouble("s").toFloat(), o.getInt("p"), o.optInt("n", 0)
                )
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveSlots(c: Context, list: List<Slot>) {
        val a = JSONArray()
        list.forEach {
            a.put(
                JSONObject().put("x", it.x.toDouble()).put("y", it.y.toDouble())
                    .put("t", it.type.name).put("s", it.seconds.toDouble()).put("p", it.percent).put("n", it.no)
            )
        }
        prefs(c).edit().putString("slots", a.toString()).apply()
    }

    // Yönetici şartı: sürekli çalışmasın. Çalışma süresi sonunda zorunlu mola.
    fun sessionMin(c: Context) = prefs(c).getInt("session", 30).coerceIn(5, 120)
    fun breakMin(c: Context) = prefs(c).getInt("break", 10).coerceIn(2, 60)
    fun saveTimes(c: Context, session: Int, brk: Int) {
        prefs(c).edit().putInt("session", session.coerceIn(5, 120)).putInt("break", brk.coerceIn(2, 60)).apply()
    }
}

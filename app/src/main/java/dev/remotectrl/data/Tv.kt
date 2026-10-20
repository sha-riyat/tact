package dev.remotectrl.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Tv(val name: String, val host: String)

/** Remembers the TVs the user has already connected to. */
class TvStore(context: Context) {
    private val prefs = context.getSharedPreferences("tvs", Context.MODE_PRIVATE)

    fun load(): List<Tv> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map {
                val o = array.getJSONObject(it)
                Tv(o.getString("name"), o.getString("host"))
            }
        }.getOrDefault(emptyList())
    }

    fun save(tv: Tv) {
        val all = load().filterNot { it.host == tv.host } + tv
        write(all)
    }

    fun remove(tv: Tv) = write(load().filterNot { it.host == tv.host })

    private fun write(list: List<Tv>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("name", it.name).put("host", it.host)) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "list"
    }
}

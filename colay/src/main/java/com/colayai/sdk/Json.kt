package com.colayai.sdk

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object Json {

    private val iso = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun isoNow(): String = iso(System.currentTimeMillis())

    fun iso(millis: Long): String = iso.get()!!.format(Date(millis))

    /** Converts a Kotlin value to a JSON-compatible value. Unsupported types become strings. */
    fun wrap(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is String, is Boolean, is Int, is Long, is Double -> value
        is Float -> value.toDouble()
        is Short -> value.toInt()
        is Byte -> value.toInt()
        is Number -> value.toDouble()
        is Date -> iso(value.time)
        is Map<*, *> -> obj(value)
        is Iterable<*> -> JSONArray().apply { value.forEach { put(wrap(it)) } }
        is Array<*> -> JSONArray().apply { value.forEach { put(wrap(it)) } }
        is JSONObject, is JSONArray -> value
        else -> value.toString()
    }

    fun obj(map: Map<*, *>): JSONObject {
        val o = JSONObject()
        for ((k, v) in map) {
            if (k == null) continue
            o.put(k.toString(), wrap(v))
        }
        return o
    }

    /** Trims properties to protocol limits: at most 64 keys, strings up to 1000 characters. */
    fun limitProps(map: Map<String, Any?>): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in map) {
            if (out.size >= 64) break
            val key = k.take(64)
            out[key] = if (v is String && v.length > 1000) v.take(1000) else v
        }
        return out
    }
}

package com.colayai.sdk

import org.json.JSONObject
import java.io.File

/**
 * Persistent on-device event queue. Each line is one event (JSON). Events survive app restarts.
 * Called only from the SDK's single worker thread.
 */
internal class EventQueue(private val file: File, private val maxSize: Int) {

    private val items = ArrayList<JSONObject>()

    init {
        load()
    }

    val size: Int get() = items.size

    fun add(event: JSONObject) {
        items.add(event)
        while (items.size > maxSize) items.removeAt(0) // the oldest event is dropped
        save()
    }

    fun peek(n: Int, maxBytes: Int): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        var bytes = 0
        for (e in items) {
            if (out.size >= n) break
            val len = e.toString().length
            if (out.isNotEmpty() && bytes + len > maxBytes) break
            out.add(e)
            bytes += len
        }
        return out
    }

    fun remove(sent: List<JSONObject>) {
        if (sent.isEmpty()) return
        val ids = sent.map { it.optString("id") }.toHashSet()
        items.removeAll { it.optString("id") in ids }
        save()
    }

    fun clear() {
        items.clear()
        save()
    }

    private fun load() {
        try {
            if (!file.exists()) return
            file.forEachLine { line ->
                if (line.isNotBlank()) {
                    try {
                        items.add(JSONObject(line))
                    } catch (_: Exception) { /* skip corrupt lines */ }
                }
            }
            while (items.size > maxSize) items.removeAt(0)
        } catch (_: Exception) {
            items.clear()
        }
    }

    private fun save() {
        try {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.bufferedWriter().use { w ->
                for (e in items) {
                    w.write(e.toString())
                    w.write("\n")
                }
            }
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (_: Exception) { /* disk error: keep events in memory */ }
    }
}

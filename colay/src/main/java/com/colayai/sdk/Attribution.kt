package com.colayai.sdk

import java.net.URLDecoder

/** Extracts ad attribution data (UTM parameters, click IDs) from links and install referrers. */
internal object Attribution {

    /** Keys kept for attribution. */
    val KEYS = listOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term", "utm_id",
        "fbclid", "gclid", "gbraid", "wbraid", "ttclid", "ctwa_clid", "msclkid", "colay_link",
    )

    /** Click IDs that can identify a person; not sent without ads consent. */
    val CLICK_IDS = setOf("fbclid", "gclid", "gbraid", "wbraid", "ttclid", "ctwa_clid", "msclkid")

    /** Parses a query string such as `a=1&b=2`. */
    fun parseQuery(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (part in query.trimStart('?').split('&')) {
            if (part.isEmpty()) continue
            val i = part.indexOf('=')
            val k = decode(if (i >= 0) part.substring(0, i) else part)
            val v = if (i >= 0) decode(part.substring(i + 1)) else ""
            if (k.isNotEmpty() && !out.containsKey(k)) out[k] = v
        }
        return out
    }

    /** Returns only the attribution keys from a query string. */
    fun extract(query: String?): Map<String, String> =
        parseQuery(query).filterKeys { it in KEYS }.filterValues { it.isNotEmpty() }

    /** Returns the attribution keys from a full URL (`scheme://path?a=b#x`). */
    fun fromUrl(url: String?): Map<String, String> {
        if (url.isNullOrBlank()) return emptyMap()
        val q = url.substringAfter('?', "").substringBefore('#')
        return extract(q)
    }

    /** Removes click IDs when there is no ads consent. */
    fun stripClickIds(attr: Map<String, Any?>): Map<String, Any?> = attr.filterKeys { it !in CLICK_IDS }

    private fun decode(s: String): String = try {
        URLDecoder.decode(s, "UTF-8")
    } catch (_: Exception) {
        s
    }
}

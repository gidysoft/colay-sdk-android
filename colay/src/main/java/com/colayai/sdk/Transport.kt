package com.colayai.sdk

import java.net.HttpURLConnection
import java.net.URL

/** What a server response means for the SDK (see PROTOCOL.md, "Responses and retries"). */
internal sealed class SendResult {
    object Ok : SendResult()
    /** Malformed batch: events are dropped. */
    object Drop : SendResult()
    /** Invalid key: events are dropped and sending stops. */
    object Unauthorized : SendResult()
    /** Batch too large: split and retried. */
    object TooLarge : SendResult()
    /** Retry later. [retryAfterSeconds] is set when the server provides it. */
    data class Retry(val retryAfterSeconds: Long?) : SendResult()
}

internal class Transport(baseUrl: String, private val key: String) {

    private val endpoint = URL(baseUrl.trimEnd('/') + "/cx/v1/batch")

    fun send(body: String): SendResult {
        var conn: HttpURLConnection? = null
        return try {
            conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-Colay-Key", key)
                setRequestProperty("User-Agent", "colay-android/${BuildConfig.SDK_VERSION}")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            try {
                (if (code >= 400) conn.errorStream else conn.inputStream)?.use { it.readBytes() }
            } catch (_: Exception) { }
            when {
                code in 200..299 -> SendResult.Ok
                code == 401 || code == 403 -> SendResult.Unauthorized
                code == 413 -> SendResult.TooLarge
                code == 429 || code >= 500 -> SendResult.Retry(conn.getHeaderField("Retry-After")?.toLongOrNull())
                else -> SendResult.Drop
            }
        } catch (_: Exception) {
            SendResult.Retry(null)
        } finally {
            conn?.disconnect()
        }
    }
}

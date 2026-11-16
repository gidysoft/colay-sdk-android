package com.colayai.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EventQueueTest {

    private fun ev(id: String) = JSONObject().put("id", id).put("name", "e$id")

    @Test
    fun persistsAndDropsOldestAtLimit() {
        val dir = Files.createTempDirectory("colayq").toFile()
        val f = File(dir, "q.jsonl")
        val q = EventQueue(f, maxSize = 3)
        (1..5).forEach { q.add(ev("$it")) }
        assertEquals(3, q.size)

        val again = EventQueue(f, maxSize = 3) // app restarted
        assertEquals(listOf("3", "4", "5"), again.peek(10, 100_000).map { it.getString("id") })
    }

    @Test
    fun sentEventsAreRemoved() {
        val dir = Files.createTempDirectory("colayq").toFile()
        val q = EventQueue(File(dir, "q.jsonl"), maxSize = 100)
        (1..5).forEach { q.add(ev("$it")) }
        val batch = q.peek(2, 100_000)
        q.remove(batch)
        assertEquals(listOf("3", "4", "5"), q.peek(10, 100_000).map { it.getString("id") })
    }

    @Test
    fun byteLimit() {
        val dir = Files.createTempDirectory("colayq").toFile()
        val q = EventQueue(File(dir, "q.jsonl"), maxSize = 100)
        (1..10).forEach { q.add(ev("$it").put("pad", "x".repeat(100))) }
        val one = q.peek(10, 150)
        assertEquals(1, one.size) // the first event is sent alone even if it exceeds the limit
    }
}
